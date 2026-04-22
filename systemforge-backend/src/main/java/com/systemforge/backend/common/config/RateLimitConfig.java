package com.systemforge.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurable rate limit properties.
 *
 * <p>Externalized to application.yaml for per-environment tuning.
 * Keyed by endpoint category — each defines a bucket capacity and
 * refill window.
 *
 * <p>Usage in application.yaml:
 * <pre>
 * app:
 *   rate-limit:
 *     otp:
 *       capacity: 5
 *       refill-minutes: 15
 *     login:
 *       capacity: 10
 *       refill-minutes: 5
 *     generate:
 *       capacity: 3
 *       refill-minutes: 60
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    private BucketSpec otp = new BucketSpec(5, 15);
    private BucketSpec login = new BucketSpec(10, 5);
    private BucketSpec generate = new BucketSpec(3, 60);
    private BucketSpec architectChat = new BucketSpec(10, 10);

    @Getter
    @Setter
    public static class BucketSpec {
        /** Maximum number of requests allowed in the refill window. */
        private int capacity;
        /** Refill window in minutes. After this period, the bucket is fully replenished. */
        private int refillMinutes;

        public BucketSpec() {
            this(10, 5);
        }

        public BucketSpec(int capacity, int refillMinutes) {
            this.capacity = capacity;
            this.refillMinutes = refillMinutes;
        }
    }
}
