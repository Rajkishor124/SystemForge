package com.systemforge.backend.notification.service.impl;

import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.notification.dto.InAppNotificationDto;
import com.systemforge.backend.notification.entity.InAppNotification;
import com.systemforge.backend.notification.repository.InAppNotificationRepository;
import com.systemforge.backend.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationServiceImpl implements InAppNotificationService {

    private final InAppNotificationRepository repository;

    @Override
    @Transactional
    public void createNotification(UUID userId, String title, String message, String type, String link) {
        log.info("Creating in-app notification for user: {}", userId);
        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .isRead(false)
                .build();
        repository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotificationDto> getUserNotifications(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        InAppNotification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIF_001", "Notification not found"));
        
        // Ensure the notification belongs to the user
        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("NOTIF_002", "Notification not found for user");
        }

        notification.setRead(true);
        repository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<InAppNotification> unreadNotifications = repository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(n -> n.setRead(true));
        repository.saveAll(unreadNotifications);
    }

    private InAppNotificationDto toDto(InAppNotification entity) {
        return InAppNotificationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .link(entity.getLink())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
