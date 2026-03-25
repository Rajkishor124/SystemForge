package com.systemforge.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Aggregated platform statistics for the admin dashboard.
 *
 * <p>Phase 2: These numbers will be cached in Redis with a 60-second TTL
 * to avoid expensive COUNT queries on every dashboard load.
 */
@Getter
@Builder
public class PlatformStatsDto {

    private final long totalUsers;
    private final long activeUsers;
    private final long totalSystemConfigs;
    private final long generatedArchitectures;
    private final long totalTemplates;
    private final long totalNotificationsSent;

    /** Timestamp of when these stats were computed — useful for cache freshness display. */
    private final LocalDateTime computedAt;
}