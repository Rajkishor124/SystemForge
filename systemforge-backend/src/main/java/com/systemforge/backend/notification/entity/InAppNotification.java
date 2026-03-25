package com.systemforge.backend.notification.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "in_app_notifications",
        indexes = {
                @Index(name = "idx_in_app_notif_user_id", columnList = "user_id"),
                @Index(name = "idx_in_app_notif_is_read", columnList = "is_read"),
                @Index(name = "idx_in_app_notif_created_at", columnList = "created_at DESC")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // e.g., "SYSTEM", "SUCCESS", "WARNING", "INFO"

    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;
}
