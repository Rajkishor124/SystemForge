package com.systemforge.backend.auth.service.impl;

import com.systemforge.backend.auth.dto.request.*;
import com.systemforge.backend.auth.dto.response.AuthResponse;
import com.systemforge.backend.auth.entity.OtpRecord;
import com.systemforge.backend.auth.entity.RefreshToken;
import com.systemforge.backend.auth.repository.OtpRecordRepository;
import com.systemforge.backend.auth.repository.RefreshTokenRepository;
import com.systemforge.backend.auth.service.JwtService;
import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.DuplicateResourceException;
import com.systemforge.backend.notification.service.NotificationService;
import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private OtpRecordRepository otpRecordRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private final String EMAIL = "test@example.com";
    private final String RAW_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .email(EMAIL)
                .password("encoded_password")
                .role(UserRole.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        activeUser.setId(UUID.randomUUID());
    }

    private void mockTokenIssuance() {
        lenient().when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access.token.mock");
        lenient().when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh.token.mock");
        lenient().when(jwtService.extractExpiry(any())).thenReturn(java.time.Instant.ofEpochMilli(System.currentTimeMillis() + 3600000L));
    }

    // ================= REGISTER =================

    @Test
    void register_success_savesUserAndReturnsTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail(EMAIL);
        req.setPassword(RAW_PASSWORD);

        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        mockTokenIssuance();

        AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("access.token.mock", response.getAccessToken());
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_emailExists_throwsDuplicateResourceException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(EMAIL);

        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    // ================= LOGIN =================

    @Test
    void login_success_returnsTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword(RAW_PASSWORD);

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(RAW_PASSWORD, "encoded_password")).thenReturn(true);
        mockTokenIssuance();

        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertNotNull(activeUser.getLastLoginAt());
        assertEquals("access.token.mock", response.getAccessToken());
    }

    @Test
    void login_inactiveAccount_throwsException() {
        activeUser.setAccountStatus(AccountStatus.SUSPENDED);
        LoginRequest req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword(RAW_PASSWORD);

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(activeUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("AUTH_009", ex.getErrorCode()); // Account is not active
    }

    // ================= OTP =================

    @Test
    void sendOtp_success_invalidatesOldAndSendsNew() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail(EMAIL);

        when(passwordEncoder.encode(anyString())).thenReturn("encoded_otp");

        authService.sendOtp(req);

        verify(otpRecordRepository).invalidateAllForEmail(EMAIL);
        
        ArgumentCaptor<OtpRecord> captor = ArgumentCaptor.forClass(OtpRecord.class);
        verify(otpRecordRepository).save(captor.capture());
        
        OtpRecord saved = captor.getValue();
        assertEquals(EMAIL, saved.getEmail());
        assertFalse(saved.isUsed());
        assertEquals("encoded_otp", saved.getOtpHash());

        verify(notificationService).sendOtpEmail(eq(EMAIL), anyString());
    }

    @Test
    void verifyOtp_success_autoRegistersAndReturnsTokens() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");

        OtpRecord otpRecord = new OtpRecord();
        otpRecord.setAttempts(0);
        otpRecord.setOtpHash("encoded_otp");

        when(otpRecordRepository.findTopByEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq(EMAIL), any()))
            .thenReturn(Optional.of(otpRecord));
            
        when(passwordEncoder.matches("123456", "encoded_otp")).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty()); // simulate new user
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        mockTokenIssuance();

        AuthResponse response = authService.verifyOtp(req);

        assertNotNull(response);
        assertTrue(otpRecord.isUsed());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_maxAttempts_throwsException() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("wrong_otp");

        OtpRecord otpRecord = new OtpRecord();
        otpRecord.setAttempts(5); // limit is 5, adding 1 = 6, which throws

        when(otpRecordRepository.findTopByEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq(EMAIL), any()))
            .thenReturn(Optional.of(otpRecord));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.verifyOtp(req));
        assertEquals("AUTH_005", ex.getErrorCode());
        assertTrue(otpRecord.isUsed());
    }

    // ================= REFRESH =================

    @Test
    void refreshToken_success_returnsNewTokens() {
        String rawRefresh = "refresh_string";
        
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUserId(activeUser.getId());
        storedToken.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        storedToken.setIssuedIp("127.0.0.1");

        // Use anyString() instead of manual hash verification to avoid complex mocking of static util
        // The token util throws, but let's assume hash(rawRefresh) gives "hashed_refresh"
        // Wait, TokenHashUtil.hash() is statically imported or called. We can't mock the static method easily without mockito-inline and MockedStatic
        // We ensure we just let the actual hash function run.
        
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(anyString())).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        mockTokenIssuance();

        AuthResponse res = authService.refreshToken(rawRefresh);

        assertNotNull(res);
        assertTrue(storedToken.isRevoked()); // old token is revoked
        verify(refreshTokenRepository).save(any(RefreshToken.class)); // new token is saved
    }

    @Test
    void logout_revokesTokens() {
        authService.logout(activeUser.getId().toString());
        verify(refreshTokenRepository).revokeAllByUserId(activeUser.getId());
    }
}
