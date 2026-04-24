package com.systemforge.backend.system.event;

import java.util.UUID;

/**
 * Event published when a new async generation job is saved to the database.
 * 
 * <p>Listened to by workers via @TransactionalEventListener to ensure
 * async execution only begins AFTER the initial PENDING job is safely
 * committed to the database.
 */
public record GenerationJobSubmittedEvent(UUID jobId) {
}
