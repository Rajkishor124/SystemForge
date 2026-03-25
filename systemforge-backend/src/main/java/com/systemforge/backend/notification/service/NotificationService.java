package com.systemforge.backend.notification.service;

import java.util.UUID;

/**
 * Notification dispatch contract — used by other modules to send notifications.
 *
 * <p>This is the ONLY way other modules interact with the notification system.
 * Direct use of repositories or providers from outside this module is prohibited.
 *
 * <p>All methods are fire-and-forget (void). Delivery tracking happens internally
 * via {@link com.systemforge.backend.notification.entity.NotificationEvent} records.
 *
 * <p>Phase 2: These calls will publish events to Kafka instead of dispatching directly,
 * enabling async delivery and horizontal scaling of the notification consumer.
 */
public interface NotificationService {

    /**
     * Sends a transactional email (OTP, welcome, password reset).
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    HTML or plain text email body
     * @param userId  owning user UUID (nullable for pre-registration emails)
     */
    void sendEmail(String to, String subject, String body, UUID userId);

    /**
     * Sends an SMS message (OTP delivery, alerts).
     *
     * @param phoneNumber E.164 formatted phone number (+91XXXXXXXXXX)
     * @param message     SMS body — max 160 chars for single segment
     * @param userId      owning user UUID
     */
    void sendSms(String phoneNumber, String message, UUID userId);

    /**
     * Sends a push notification to a registered device.
     *
     * @param deviceToken FCM or APNs device registration token
     * @param title       notification title
     * @param body        notification body text
     * @param userId      owning user UUID
     */
    void sendPush(String deviceToken, String title, String body, UUID userId);

    /**
     * Convenience method for OTP email — applies the standard OTP email template.
     *
     * @param email the recipient email
     * @param otp   the raw 6-digit OTP (never stored — only sent)
     */
    void sendOtpEmail(String email, String otp);
}