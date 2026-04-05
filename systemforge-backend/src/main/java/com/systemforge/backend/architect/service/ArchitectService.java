package com.systemforge.backend.architect.service;

import com.systemforge.backend.architect.dto.ArchitectRequest;
import com.systemforge.backend.architect.dto.ArchitectResponse;

import java.util.List;
import java.util.UUID;

/**
 * AI Architect Agent service contract.
 */
public interface ArchitectService {

    /** Process a design request and return structured response. */
    ArchitectResponse process(ArchitectRequest request);

    /** List sessions for the authenticated user. */
    List<ArchitectSessionDto> listSessions();

    /** Get full session with messages. */
    ArchitectSessionDetailDto getSession(UUID sessionId);

    /** Delete a session. */
    void deleteSession(UUID sessionId);

    // ─── Inner DTOs ────────────────────────────────────────────────────────

    record ArchitectSessionDto(
            UUID id, String title, String intent, String status,
            long messageCount, String lastMessagePreview,
            java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt
    ) {}

    record ArchitectSessionDetailDto(
            UUID id, String title, String intent, String status,
            java.time.LocalDateTime createdAt,
            List<ArchitectMessageDto> messages
    ) {}

    record ArchitectMessageDto(
            UUID id, String role, String content, String source,
            String intent, Long processingTimeMs,
            java.time.LocalDateTime createdAt,
            List<com.systemforge.backend.architect.dto.AgentStep> steps
    ) {}
}
