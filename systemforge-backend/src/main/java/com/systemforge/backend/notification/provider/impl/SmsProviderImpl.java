package com.systemforge.backend.notification.provider.impl;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.provider.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SmsProviderImpl implements NotificationProvider {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.SMS == channel;
    }

    @Override
    public String send(NotificationEvent event) {
        log.info("[SmsProvider] Sending SMS to {} | Body: {}", event.getRecipient(), event.getBody());
        // TODO: Phase 2 - Integrate Twilio / AWS SNS
        return "sms-id-" + UUID.randomUUID().toString();
    }
}
