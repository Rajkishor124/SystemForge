package com.systemforge.backend.notification.service.impl;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.enums.NotificationStatus;
import com.systemforge.backend.notification.provider.NotificationProvider;
import com.systemforge.backend.notification.repository.NotificationEventRepository;
import com.systemforge.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Notification service implementation - Strategy Driven.
 *
 * <p>Persists notification events to the audit log and dynamically routes them
 * to the appropriate {@link NotificationProvider}.
 *
 * <p><strong>Phase 2 implementation plan:</strong>
 * <ul>
 *   <li>Email: Spring Mail + AWS SES or SendGrid</li>
 *   <li>SMS: Twilio SDK or AWS SNS</li>
 *   <li>Push: Firebase Admin SDK (FCM)</li>
 *   <li>Retry: @Scheduled job polling FAILED events with retryCount &lt; 3</li>
 *   <li>Async: Move dispatch to Kafka consumer (Phase 3)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NotificationServiceImpl implements NotificationService {

    private static final String OTP_EMAIL_SUBJECT = "Your SystemForge OTP";
    private static final String OTP_EMAIL_TEMPLATE =
            "Your OTP is: <strong>%s</strong>. Valid for 10 minutes. Do not share this with anyone.";

    private final NotificationEventRepository notificationEventRepository;
    private final List<NotificationProvider> providers;

    @Override
    public void sendEmail(String to, String subject, String body, UUID userId) {
        log.info("Dispatching email to={} subject='{}'", to, subject);

        NotificationEvent event = persistEvent(
                userId, to, NotificationChannel.EMAIL, subject, body);

        dispatch(event);
    }

    @Override
    public void sendSms(String phoneNumber, String message, UUID userId) {
        log.info("Dispatching SMS to={}", phoneNumber);

        NotificationEvent event = persistEvent(
                userId, phoneNumber, NotificationChannel.SMS, null, message);

        dispatch(event);
    }

    @Override
    public void sendPush(String deviceToken, String title, String body, UUID userId) {
        log.info("Dispatching push notification to deviceToken={}...", deviceToken.substring(0, 8));

        NotificationEvent event = persistEvent(
                userId, deviceToken, NotificationChannel.PUSH, title, body);

        dispatch(event);
    }

    @Override
    public void sendOtpEmail(String email, String otp) {
        String body = String.format(OTP_EMAIL_TEMPLATE, otp);
        sendEmail(email, OTP_EMAIL_SUBJECT, body, null);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private NotificationEvent persistEvent(UUID userId, String recipient,
                                           NotificationChannel channel,
                                           String subject, String body) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .recipient(recipient)
                .channel(channel)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
        return notificationEventRepository.save(event);
    }

    private void dispatch(NotificationEvent event) {
        NotificationProvider matchedProvider = providers.stream()
                .filter(p -> p.supports(event.getChannel()))
                .findFirst()
                .orElse(null);

        if (matchedProvider == null) {
            log.error("No notification provider found for channel: {}", event.getChannel());
            markFailed(event, "No provider configuration found");
            return;
        }

        try {
            String messageId = matchedProvider.send(event);
            markSent(event, messageId);
        } catch (Exception e) {
            log.error("Provider dispatch failed for channel {}", event.getChannel(), e);
            markFailed(event, e.getMessage());
        }
    }

    private void markSent(NotificationEvent event, String providerMessageId) {
        event.setStatus(NotificationStatus.SENT);
        event.setProviderMessageId(providerMessageId);
        notificationEventRepository.save(event);
    }

    private void markFailed(NotificationEvent event, String reason) {
        event.setStatus(NotificationStatus.FAILED);
        event.setFailureReason(reason);
        event.setRetryCount(event.getRetryCount() + 1);
        notificationEventRepository.save(event);
    }
}