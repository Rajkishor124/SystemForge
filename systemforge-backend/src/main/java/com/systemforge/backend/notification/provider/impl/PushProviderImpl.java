package com.systemforge.backend.notification.provider.impl;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.provider.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class PushProviderImpl implements NotificationProvider {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.PUSH == channel;
    }

    @Override
    public String send(NotificationEvent event) {
        log.info("[PushProvider] Sending push notification to {} | Title: {}", event.getRecipient(), event.getSubject());
        // TODO: Phase 2 - Integrate Firebase FCM
        return "fcm-id-" + UUID.randomUUID().toString();
    }
}
