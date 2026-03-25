package com.systemforge.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationDto {
    private UUID id;
    private String title;
    private String message;
    private String type;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
}
