package com.systemforge.backend.notification.provider.impl;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.provider.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class EmailProviderImpl implements NotificationProvider {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.EMAIL == channel;
    }

    @Override
    public String send(NotificationEvent event) {
        log.info("[EmailProvider] Sending email to {} | Subject: {}", event.getRecipient(), event.getSubject());
        // TODO: Phase 2 - Integrate AWS SES / Spring Mail
        // Generate a mock message ID
        return "email-id-" + UUID.randomUUID().toString();
    }
}
