package com.systemforge.backend.playground.cache;

import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;

import java.util.Optional;

/**
 * Cache contract for playground generation output.
 *
 * <p>MVP uses in-memory implementation.
 * Designed for drop-in Redis replacement — just swap the bean.
 */
public interface OutputCacheService {

    Optional<PlaygroundGeneratedOutput> get(String cacheKey);

    void put(String cacheKey, PlaygroundGeneratedOutput output);

    /**
     * Builds a deterministic cache key from the full config JSON.
     * Future-proof: supports any new config fields without key changes.
     */
    String buildKey(String configJson);
}
