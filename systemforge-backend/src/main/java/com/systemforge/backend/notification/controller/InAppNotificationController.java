package com.systemforge.backend.notification.controller;

import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.notification.dto.InAppNotificationDto;
import com.systemforge.backend.notification.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "In-App Notifications", description = "Endpoints for managing user in-app notifications")
public class InAppNotificationController {

    private final InAppNotificationService notificationService;
    private final SecurityService securityService;

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Fetch a paginated list of notifications for the authenticated user")
    public ResponseEntity<ApiResponse<PagedResponse<InAppNotificationDto>>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        UUID userId = securityService.getAuthenticatedUserId();

        Page<InAppNotificationDto> notifications = notificationService.getUserNotifications(
                userId, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(
                "Notifications retrieved successfully",
                PagedResponse.from(notifications, dto -> dto)
        ));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get the total count of unread notifications for the user")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        UUID userId = securityService.getAuthenticatedUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        UUID userId = securityService.getAuthenticatedUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        UUID userId = securityService.getAuthenticatedUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
