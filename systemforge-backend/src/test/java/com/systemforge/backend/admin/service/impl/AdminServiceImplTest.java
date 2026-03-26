package com.systemforge.backend.admin.service.impl;

import com.systemforge.backend.admin.dto.PlatformStatsDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserSystemConfigRepository userSystemConfigRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private NotificationEventRepository notificationEventRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.DEVELOPER);
        testUser.setAccountStatus(AccountStatus.SUSPENDED);
    }

    @Test
    void getPlatformStats_Success() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(80L);
        when(userSystemConfigRepository.count()).thenReturn(500L);
        when(userSystemConfigRepository.countByIsGeneratedTrue()).thenReturn(450L);
        when(templateRepository.count()).thenReturn(20L);
        when(notificationEventRepository.count()).thenReturn(1000L);

        // Act
        PlatformStatsDto stats = adminService.getPlatformStats();

        // Assert
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalUsers());
        assertEquals(80L, stats.getActiveUsers());
        assertEquals(500L, stats.getTotalSystemConfigs());
        assertEquals(450L, stats.getGeneratedArchitectures());
        assertEquals(20L, stats.getTotalTemplates());
        assertEquals(1000L, stats.getTotalNotificationsSent());
        assertNotNull(stats.getComputedAt());

        verify(userRepository).count();
        verify(userRepository).countByAccountStatus(AccountStatus.ACTIVE);
        verify(userSystemConfigRepository).count();
        verify(userSystemConfigRepository).countByIsGeneratedTrue();
        verify(templateRepository).count();
        verify(notificationEventRepository).count();
    }

    @Test
    void activateUser_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserResponse activeResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .role(UserRole.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        
        when(userMapper.toDto(any(User.class))).thenReturn(activeResponse);

        // Act
        UserResponse response = adminService.activateUser(userId);

        // Assert
        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        assertEquals(AccountStatus.ACTIVE, testUser.getAccountStatus());
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void activateUser_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> adminService.activateUser(userId));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUser_Success() {
        // Arrange
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserResponse deactivatedResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .role(UserRole.DEVELOPER)
                .accountStatus(AccountStatus.DEACTIVATED)
                .build();
        
        when(userMapper.toDto(any(User.class))).thenReturn(deactivatedResponse);

        // Act
        UserResponse response = adminService.deactivateUser(userId);

        // Assert
        assertNotNull(response);
        assertEquals(AccountStatus.DEACTIVATED, response.getAccountStatus());
        assertEquals(AccountStatus.DEACTIVATED, testUser.getAccountStatus());
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void changeUserRole_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserResponse adminRoleResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .role(UserRole.ADMIN)
                .accountStatus(AccountStatus.SUSPENDED)
                .build();
                
        when(userMapper.toDto(any(User.class))).thenReturn(adminRoleResponse);

        // Act
        UserResponse response = adminService.changeUserRole(userId, "ADMIN");

        // Assert
        assertNotNull(response);
        assertEquals(UserRole.ADMIN, response.getRole());
        assertEquals(UserRole.ADMIN, testUser.getRole());
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void changeUserRole_InvalidRole_ThrowsException() {
        // Arrange
        String invalidRole = "SUPER_ADMIN";

        // Act & Assert
        BusinessException e = assertThrows(BusinessException.class, 
                () -> adminService.changeUserRole(userId, invalidRole));
        
        assertEquals("ADM_001", e.getErrorCode());
        assertTrue(e.getMessage().contains("Invalid role"));
        
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
}
