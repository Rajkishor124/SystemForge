package com.systemforge.backend.notification.service.impl;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationChannel;
import com.systemforge.backend.notification.enums.NotificationStatus;
import com.systemforge.backend.notification.provider.NotificationProvider;
import com.systemforge.backend.notification.repository.NotificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationEventRepository repository;

    @Mock
    private NotificationProvider emailProvider;

    @Mock
    private NotificationProvider smsProvider;

    private NotificationServiceImpl notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(emailProvider.supports(NotificationChannel.EMAIL)).thenReturn(true);
        lenient().when(emailProvider.supports(NotificationChannel.SMS)).thenReturn(false);
        lenient().when(emailProvider.supports(NotificationChannel.PUSH)).thenReturn(false);
        
        // We only mock supports() strictly for the channels we use per test
        // Let's configure the provider list
        notificationService = new NotificationServiceImpl(
                repository,
                List.of(emailProvider, smsProvider)
        );
    }

    @Test
    void sendEmail_success_setsStatusSent() {
        // Arrange
        NotificationEvent initialEvent = new NotificationEvent();
        initialEvent.setId(UUID.randomUUID());
        initialEvent.setChannel(NotificationChannel.EMAIL);
        initialEvent.setStatus(NotificationStatus.PENDING);
        initialEvent.setRetryCount(0);
        
        when(repository.save(any(NotificationEvent.class))).thenReturn(initialEvent);
        when(emailProvider.send(any(NotificationEvent.class))).thenReturn("msg-123");

        // Act
        notificationService.sendEmail("test@example.com", "Subject", "Body", userId);

        // Assert
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(repository, times(2)).save(captor.capture());
        
        NotificationEvent savedEvent = captor.getAllValues().get(1); // The second save is the update
        assertEquals(NotificationStatus.SENT, savedEvent.getStatus());
        assertEquals("msg-123", savedEvent.getProviderMessageId());
        verify(emailProvider).send(any(NotificationEvent.class));
        verify(smsProvider, never()).send(any());
    }

    @Test
    void sendSms_success_setsStatusSent() {
        // Arrange
        when(smsProvider.supports(NotificationChannel.SMS)).thenReturn(true);
        
        NotificationEvent initialEvent = new NotificationEvent();
        initialEvent.setId(UUID.randomUUID());
        initialEvent.setChannel(NotificationChannel.SMS);
        initialEvent.setStatus(NotificationStatus.PENDING);
        initialEvent.setRetryCount(0);
        
        when(repository.save(any(NotificationEvent.class))).thenReturn(initialEvent);
        when(smsProvider.send(any(NotificationEvent.class))).thenReturn("sms-456");

        // Act
        notificationService.sendSms("+1234567890", "Test SMS", userId);

        // Assert
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(repository, times(2)).save(captor.capture());
        
        NotificationEvent savedEvent = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.SENT, savedEvent.getStatus());
        assertEquals("sms-456", savedEvent.getProviderMessageId());
        verify(smsProvider).send(any(NotificationEvent.class));
    }

    @Test
    void dispatch_providerNotFound_setsStatusFailed() {
        // Arrange
        // We are triggering PUSH, but neither emailProvider nor smsProvider supports PUSH.
        when(emailProvider.supports(NotificationChannel.PUSH)).thenReturn(false);
        when(smsProvider.supports(NotificationChannel.PUSH)).thenReturn(false);
        
        NotificationEvent initialEvent = new NotificationEvent();
        initialEvent.setId(UUID.randomUUID());
        initialEvent.setChannel(NotificationChannel.PUSH);
        initialEvent.setStatus(NotificationStatus.PENDING);
        initialEvent.setRetryCount(0);
        
        when(repository.save(any(NotificationEvent.class))).thenReturn(initialEvent);

        // Act
        notificationService.sendPush("device-token", "Title", "Body", userId);

        // Assert
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(repository, times(2)).save(captor.capture());
        
        NotificationEvent savedEvent = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.FAILED, savedEvent.getStatus());
        assertEquals("No provider configuration found", savedEvent.getFailureReason());
        assertEquals(1, savedEvent.getRetryCount());
        
        verify(emailProvider, never()).send(any());
        verify(smsProvider, never()).send(any());
    }

    @Test
    void dispatch_providerThrowsException_setsStatusFailed() {
        // Arrange
        NotificationEvent initialEvent = new NotificationEvent();
        initialEvent.setId(UUID.randomUUID());
        initialEvent.setChannel(NotificationChannel.EMAIL);
        initialEvent.setStatus(NotificationStatus.PENDING);
        initialEvent.setRetryCount(0);
        
        when(repository.save(any(NotificationEvent.class))).thenReturn(initialEvent);
        when(emailProvider.send(any(NotificationEvent.class))).thenThrow(new RuntimeException("API Timeout"));

        // Act
        notificationService.sendEmail("test@example.com", "Subject", "Body", userId);

        // Assert
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(repository, times(2)).save(captor.capture());
        
        NotificationEvent savedEvent = captor.getAllValues().get(1);
        assertEquals(NotificationStatus.FAILED, savedEvent.getStatus());
        assertEquals("API Timeout", savedEvent.getFailureReason());
        assertEquals(1, savedEvent.getRetryCount());
    }
}
