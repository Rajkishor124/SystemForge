package com.systemforge.backend.notification.provider;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;

/**
 * Strategy interface for diverse notification dispatch mechanisms.
 */
public interface NotificationProvider {

    /**
     * Determines if this provider is capable of handling the specified channel.
     * @param channel the channel to test
     * @return true if supported
     */
    boolean supports(NotificationChannel channel);

    /**
     * Executes the actual dispatch to the external service.
     *
     * @param event the parsed notification to send
     * @return provider-specific message ID on success
     * @throws RuntimeException (or ProviderException) if dispatch fails
     */
    String send(NotificationEvent event);
}
