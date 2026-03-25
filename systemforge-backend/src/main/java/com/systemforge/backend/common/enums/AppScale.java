package com.systemforge.backend.common.enums;

/**
 * Represents the target scale of the application being designed.
 *
 * <p>Used by the Recommendation Engine to suggest appropriate architecture.
 * Scale directly influences: database choice, caching strategy, deployment topology,
 * and queue vs direct-call patterns.
 */
public enum AppScale {

    /**
     * Small scale — startup / MVP phase.
     * Typical: &lt;10k users, single region, minimal infrastructure.
     * Recommend: Modular monolith, single DB, no caching layer.
     */
    SMALL,

    /**
     * Medium scale — growth phase.
     * Typical: 10k–500k users, moderate traffic spikes.
     * Recommend: Read replicas, Redis cache, async notifications via queue.
     */
    MEDIUM,

    /**
     * Large scale — enterprise / hyper-growth.
     * Typical: 500k+ users, global distribution, high availability requirements.
     * Recommend: Microservices extraction, Kafka, multi-region DB, CDN.
     */
    LARGE
}