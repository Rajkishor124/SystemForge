package com.systemforge.backend.architect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * Incoming request to the AI Architect Agent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectRequest {

    /** The user's message / design request. */
    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must be under 5000 characters")
    private String message;

    /** Optional: existing session to continue. */
    private UUID sessionId;
}
