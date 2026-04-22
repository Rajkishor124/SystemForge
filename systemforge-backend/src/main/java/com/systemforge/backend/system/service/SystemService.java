package com.systemforge.backend.system.service;

import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.system.dto.GenerationJobDto;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Core System Engine service contract.
 *
 * <p>This is the primary interface of SystemForge's main feature —
 * system selection, configuration, and architecture generation.
 */
public interface SystemService {

    // ─── System Catalog ───────────────────────────────────────────────────

    /** Returns all available, active system definitions. */
    List<SystemDefinitionDto> getAllSystems();

    /** Returns active systems filtered by type. */
    List<SystemDefinitionDto> getSystemsByType(SystemType type);

    // ─── User Config Management ───────────────────────────────────────────

    /** Creates a new architecture design session for a user. */
    UserSystemConfigDto createConfig(UUID userId, CreateSystemConfigRequest request);

    /** Returns a specific config by ID, scoped to the user. */
    UserSystemConfigDto getConfigById(UUID userId, UUID configId);

    /** Returns all configs for a user, paginated. */
    PagedResponse<UserSystemConfigDto> getUserConfigs(UUID userId, Pageable pageable);

    /** Triggers the architecture generation engine for a config (synchronous — legacy). */
    UserSystemConfigDto generateArchitecture(UUID userId, UUID configId);

    /** Soft-deletes a config. */
    void deleteConfig(UUID userId, UUID configId);

    // ─── Async Generation ─────────────────────────────────────────────────

    /**
     * Submits a generation job for async processing.
     * Returns immediately with the job reference (PENDING status).
     */
    GenerationJobDto submitGeneration(UUID userId, UUID configId);

    /**
     * Returns the current status of a generation job.
     * Includes result payload when status is COMPLETED.
     */
    GenerationJobDto getJobStatus(UUID userId, UUID jobId);
}