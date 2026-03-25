package com.systemforge.backend.user.service.impl;

import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.user.dto.request.ChangePasswordRequest;
import com.systemforge.backend.user.dto.request.UpdateProfileRequest;
import com.systemforge.backend.user.dto.response.UserResponse;
import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.mapper.UserMapper;
import com.systemforge.backend.user.repository.UserRepository;
import com.systemforge.backend.user.service.UserService;
import com.systemforge.backend.auth.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    // ================= READ =================

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID userId) {
        log.debug("Fetching user by id={}", userId);
        return userMapper.toDto(getUserOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        log.debug("Fetching user by email={}", normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USR_002", "User not found with email: " + normalizedEmail));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(Pageable pageable) {
        log.debug("Fetching users page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return PagedResponse.from(userRepository.findAll(pageable), userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID userId = securityService.getAuthenticatedUserId();
        log.debug("Fetching current user id={}", userId);
        return userMapper.toDto(getActiveUserOrThrow(userId));
    }

    // ================= WRITE =================

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        UUID userId = securityService.getAuthenticatedUserId();
        User user = getActiveUserOrThrow(userId);

        log.info("Updating profile for user id={}", userId);

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = securityService.getAuthenticatedUserId();
        User user = getActiveUserOrThrow(userId);

        log.info("Changing password for user id={}", userId);

        if (user.getPassword() == null) {
            throw new BusinessException(
                    "PASSWORD_NOT_SUPPORTED",
                    "Password change not allowed for this account",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(
                    "INVALID_PASSWORD",
                    "Current password is incorrect",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Prevent reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(
                    "PASSWORD_REUSE",
                    "New password must be different from current password",
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        log.info("Password updated successfully for user id={}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("Soft-deleting user id={}", userId);

        User user = getUserOrThrow(userId);

        if (user.isDeleted()) {
            log.warn("User already deleted id={}", userId);
            return;
        }

        user.setDeleted(true);
        userRepository.save(user);

        log.info("User soft-deleted id={}", userId);
    }

    // ================= HELPERS =================

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USR_001", "User not found with id: " + userId));
    }

    /**
     * Ensures user exists AND is ACTIVE
     */
    private User getActiveUserOrThrow(UUID userId) {
        User user = getUserOrThrow(userId);

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "ACCOUNT_INACTIVE",
                    "User account is not active",
                    HttpStatus.FORBIDDEN
            );
        }

        return user;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}