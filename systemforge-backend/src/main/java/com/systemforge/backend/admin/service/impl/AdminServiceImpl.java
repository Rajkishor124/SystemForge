package com.systemforge.backend.admin.service.impl;

import com.systemforge.backend.admin.dto.PlatformStatsDto;
import com.systemforge.backend.admin.service.AdminService;
import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.notification.repository.NotificationEventRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import com.systemforge.backend.template.repository.TemplateRepository;
import com.systemforge.backend.user.dto.response.UserResponse;
import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.mapper.UserMapper;
import com.systemforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin service implementation.
 *
 * <p>The Admin module is the ONLY module permitted to directly use repositories
 * from other modules, because admin operations are inherently cross-cutting.
 * All other inter-module communication must use service interfaces.
 *
 * <p>Phase 2: Add @PreAuthorize("hasRole('ADMIN')") to each method.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserSystemConfigRepository userSystemConfigRepository;
    private final TemplateRepository templateRepository;
    private final NotificationEventRepository notificationEventRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "platformStats")
    public PlatformStatsDto getPlatformStats() {
        log.info("Computing platform statistics");

        long totalUsers = userRepository.count();
        long totalConfigs = userSystemConfigRepository.count();
        long totalTemplates = templateRepository.count();
        long totalNotifications = notificationEventRepository.count();

        // Generated configs
        long generatedArchitectures = userSystemConfigRepository.countByIsGeneratedTrue();
        
        // Active users
        long activeUsers = userRepository.countByAccountStatus(AccountStatus.ACTIVE);

        return PlatformStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalSystemConfigs(totalConfigs)
                .generatedArchitectures(generatedArchitectures)
                .totalTemplates(totalTemplates)
                .totalNotificationsSent(totalNotifications)
                .computedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activateUser(UUID userId) {
        log.info("ADMIN: Activating user id={}", userId);
        User user = findUserOrThrow(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivateUser(UUID userId) {
        log.info("ADMIN: Deactivating user id={}", userId);
        User user = findUserOrThrow(userId);
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeUserRole(UUID userId, String newRole) {
        log.info("ADMIN: Changing role for user id={} to role={}", userId, newRole);

        UserRole role;
        try {
            role = UserRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("ADM_001",
                    "Invalid role: " + newRole + ". Valid roles: ADMIN, DEVELOPER",
                    HttpStatus.BAD_REQUEST);
        }

        User user = findUserOrThrow(userId);
        user.setRole(role);
        return userMapper.toDto(userRepository.save(user));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USR_001", "User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.data.domain.Page<UserResponse> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }
}