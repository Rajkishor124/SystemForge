package com.systemforge.backend.auth.service.impl;

import com.systemforge.backend.auth.dto.request.*;
import com.systemforge.backend.auth.dto.response.AuthResponse;
import com.systemforge.backend.auth.entity.OtpRecord;
import com.systemforge.backend.auth.entity.RefreshToken;
import com.systemforge.backend.auth.repository.OtpRecordRepository;
import com.systemforge.backend.auth.repository.RefreshTokenRepository;
import com.systemforge.backend.auth.service.AuthService;
import com.systemforge.backend.auth.service.JwtService;
import com.systemforge.backend.auth.util.OtpGenerator;
import com.systemforge.backend.auth.util.TokenHashUtil;
import com.systemforge.backend.common.enums.AuthType;
import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.DuplicateResourceException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.common.security.InputSanitizer;
import com.systemforge.backend.notification.service.NotificationService;
import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.enums.AuthProvider;
import com.systemforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuthServiceImpl implements AuthService {

    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final long REFRESH_EXPIRY_DAYS = 7L;

    private final OtpRecordRepository otpRecordRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    // ================= REGISTER =================

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("AUTH_002",
                    "An account with this email already exists");
        }

        User user = User.builder()
                .name(InputSanitizer.sanitize(request.getName()))
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .isEmailVerified(false)
                .build();

        User saved = userRepository.save(user);

        log.info("User registered id={}", saved.getId());

        return issueTokenPair(saved, null, null);
    }

    // ================= LOGIN =================

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("AUTH_003",
                        "Invalid email or password", HttpStatus.UNAUTHORIZED));

        assertUserIsActive(user);
        assertUserHasPassword(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("AUTH_003",
                    "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLoginAt(Instant.now());

        return issueTokenPair(user, null, null);
    }

    // ================= SEND OTP =================

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        otpRecordRepository.invalidateAllForEmail(email);

        String rawOtp = OtpGenerator.generate();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        OtpRecord record = OtpRecord.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(rawOtp))
                .authType(AuthType.OTP)
                .expiresAt(now.plusMinutes(OTP_VALIDITY_MINUTES))
                .isUsed(false)
                .attempts(0)
                .build();

        otpRecordRepository.save(record);

        try {
            notificationService.sendOtpEmail(email, rawOtp);
        } catch (Exception e) {
            log.error("OTP email failed for {}", email, e);
        }
    }

    // ================= VERIFY OTP =================

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        OtpRecord otpRecord = otpRecordRepository
                .findTopByEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, now)
                .orElseThrow(() -> new BusinessException("AUTH_004",
                        "OTP is invalid or expired", HttpStatus.UNAUTHORIZED));

        otpRecord.setAttempts(otpRecord.getAttempts() + 1);

        if (otpRecord.getAttempts() > OTP_MAX_ATTEMPTS) {
            otpRecord.setUsed(true);
            throw new BusinessException("AUTH_005",
                    "Too many attempts", HttpStatus.TOO_MANY_REQUESTS);
        }

        if (!passwordEncoder.matches(request.getOtp(), otpRecord.getOtpHash())) {
            throw new BusinessException("AUTH_004",
                    "Incorrect OTP", HttpStatus.UNAUTHORIZED);
        }

        otpRecord.setUsed(true);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(extractNameFromEmail(email))
                        .email(email)
                        .role(UserRole.DEVELOPER)
                        .accountStatus(AccountStatus.ACTIVE)
                        .authProvider(AuthProvider.EMAIL_OTP)
                        .isEmailVerified(true)
                        .build()));

        assertUserIsActive(user);
        user.setLastLoginAt(Instant.now());

        return issueTokenPair(user, null, null);
    }

    // ================= REFRESH =================

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {

        String tokenHash = TokenHashUtil.hash(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndIsRevokedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException("AUTH_006",
                        "Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            storedToken.setRevoked(true);
            throw new BusinessException("AUTH_007",
                    "Session expired", HttpStatus.UNAUTHORIZED);
        }

        storedToken.setRevoked(true);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("USR_001", "User not found"));

        assertUserIsActive(user);

        return issueTokenPair(user, storedToken.getIssuedIp(), storedToken.getUserAgent());
    }

    // ================= LOGOUT =================

    @Override
    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId));
    }

    // ================= TOKEN =================

    private AuthResponse issueTokenPair(User user, String ip, String agent) {

        String userId = user.getId().toString();
        String role = user.getRole().name();

        String accessToken = jwtService.generateAccessToken(userId, role, user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(userId);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHashUtil.hash(refreshToken))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(REFRESH_EXPIRY_DAYS))
                .issuedIp(ip)
                .userAgent(agent)
                .isRevoked(false)
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(jwtService.extractExpiry(jwtService.parseToken(accessToken)))
                .userId(userId)
                .role(role)
                .build();
    }

    // ================= HELPERS =================

    private void assertUserIsActive(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("AUTH_009",
                    "Account is not active", HttpStatus.FORBIDDEN);
        }
    }

    private void assertUserHasPassword(User user) {
        if (user.getPassword() == null) {
            throw new BusinessException("AUTH_011",
                    "Use OTP login instead", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String extractNameFromEmail(String email) {
        String name = email.split("@")[0];
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}