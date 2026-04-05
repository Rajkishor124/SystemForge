package com.systemforge.backend.playground.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache with bounded size and TTL eviction.
 *
 * <p>Replaces the unbounded ConcurrentHashMap to prevent memory leaks.
 * Key = SHA-256 hash of the full serialized config JSON.
 */
@Service
@Slf4j
public class InMemoryOutputCacheService implements OutputCacheService {

    private final Cache<String, PlaygroundGeneratedOutput> cache;

    public InMemoryOutputCacheService() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("Playground cache initialized: maxSize=1000, TTL=10min, stats=enabled");
    }

    @Override
    public Optional<PlaygroundGeneratedOutput> get(String cacheKey) {
        PlaygroundGeneratedOutput cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT — key={}, stats=[hitRate={}, evictions={}]",
                    cacheKey.substring(0, 12),
                    String.format("%.1f%%", cache.stats().hitRate() * 100),
                    cache.stats().evictionCount());
        } else {
            log.debug("Cache MISS — key={}", cacheKey.substring(0, 12));
        }
        return Optional.ofNullable(cached);
    }

    @Override
    public void put(String cacheKey, PlaygroundGeneratedOutput output) {
        cache.put(cacheKey, output);
        log.debug("Cache PUT — key={}, currentSize={}", cacheKey.substring(0, 12), cache.estimatedSize());
    }

    @Override
    public String buildKey(String configJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(configJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
