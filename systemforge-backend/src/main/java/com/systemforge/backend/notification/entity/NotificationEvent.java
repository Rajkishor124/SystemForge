package com.systemforge.backend.notification.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for every notification dispatched by SystemForge.
 *
 * <p>Every notification attempt is persisted — both successes and failures.
 * This enables:
 * <ul>
 *   <li>Retry logic for failed notifications</li>
 *   <li>Delivery status reporting</li>
 *   <li>Audit trail for compliance (email delivery for password resets, etc.)</li>
 * </ul>
 *
 * <p>userId is stored as UUID column (not @ManyToOne) — module isolation rule.
 */
@Entity
@Table(
        name = "notification_events",
        indexes = {
                @Index(name = "idx_notif_recipient", columnList = "recipient"),
                @Index(name = "idx_notif_status", columnList = "status"),
                @Index(name = "idx_notif_user_id", columnList = "user_id"),
                @Index(name = "idx_notif_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    /** Email address, phone number, or device token depending on the channel. */
    @Column(name = "recipient", nullable = false, length = 512)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /** External provider's message/delivery ID for status tracking. */
    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    /** Error detail if delivery failed — for debugging and retry logic. */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}