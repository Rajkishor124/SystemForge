package com.systemforge.backend.notification.service;

import com.systemforge.backend.notification.dto.InAppNotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InAppNotificationService {

    /**
     * Create a new in-app notification.
     */
    void createNotification(UUID userId, String title, String message, String type, String link);

    /**
     * Fetch paginated notifications for a user.
     */
    Page<InAppNotificationDto> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Get the count of unread notifications for a user.
     */
    long getUnreadCount(UUID userId);

    /**
     * Mark a specific notification as read.
     */
    void markAsRead(UUID notificationId, UUID userId);

    /**
     * Mark all notifications as read for a user.
     */
    void markAllAsRead(UUID userId);
}
