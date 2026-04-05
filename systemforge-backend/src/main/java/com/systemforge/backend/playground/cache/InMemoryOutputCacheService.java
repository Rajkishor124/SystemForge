package com.systemforge.backend.playground.cache;

import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for MVP. Backed by ConcurrentHashMap.
 *
 * <p>Cache key = SHA-256 hash of the full serialized config JSON.
 * This guarantees uniqueness and supports future config field additions.
 */
@Service
@Slf4j
public class InMemoryOutputCacheService implements OutputCacheService {

    private final ConcurrentHashMap<String, PlaygroundGeneratedOutput> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<PlaygroundGeneratedOutput> get(String cacheKey) {
        PlaygroundGeneratedOutput cached = cache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for key: {}", cacheKey.substring(0, 12));
        }
        return Optional.ofNullable(cached);
    }

    @Override
    public void put(String cacheKey, PlaygroundGeneratedOutput output) {
        cache.put(cacheKey, output);
        log.debug("Cache PUT for key: {} (total entries: {})", cacheKey.substring(0, 12), cache.size());
    }

    @Override
    public String buildKey(String configJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(configJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard JVMs
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
