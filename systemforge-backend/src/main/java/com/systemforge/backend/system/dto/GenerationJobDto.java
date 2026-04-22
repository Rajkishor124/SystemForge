package com.systemforge.backend.system.dto;

import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.common.enums.JobType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for exposing generation job status to clients.
 *
 * <p>The {@code resultJson} field is only populated when status is COMPLETED.
 * For PENDING/PROCESSING states, the client uses this DTO for polling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJobDto {

    private UUID id;
    private UUID configId;
    private UUID sessionId;
    private JobType jobType;
    private JobStatus status;
    private String resultJson;
    private String errorMessage;
    private String mabaMetadata;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
