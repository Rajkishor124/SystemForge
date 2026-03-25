package com.systemforge.backend.notification.enums;

/** Delivery channel for a notification. */
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,       // Firebase FCM / APNs
    IN_APP      // Real-time in-app alert (WebSocket / SSE)
}