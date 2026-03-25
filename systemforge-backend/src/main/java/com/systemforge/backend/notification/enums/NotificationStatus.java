package com.systemforge.backend.notification.enums;

/**
 * Delivery lifecycle status for a notification event.
 *
 * <p>State machine: PENDING → SENT → DELIVERED (terminal success)
 *                   PENDING → FAILED → PENDING (retry)
 *                   PENDING → FAILED (after max retries, terminal failure)
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}