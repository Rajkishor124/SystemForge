package com.systemforge.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Type-safe binding for the {@code app.*} configuration namespace.
 *
 * <p>All application-specific properties should live here rather than being
 * injected individually with {@code @Value}. Benefits:
 * <ul>
 *   <li>Validated at startup — app fails fast if required config is missing</li>
 *   <li>Single source of truth for custom configuration</li>
 *   <li>IDE auto-complete support via spring-configuration-processor</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank(message = "JWT secret must not be blank")
        private String secret;

        /** Access token expiry in milliseconds. Default: 15 minutes. */
        @Positive
        private long accessTokenExpiryMs = 900_000L;

        /** Refresh token expiry in milliseconds. Default: 7 days. */
        @Positive
        private long refreshTokenExpiryMs = 604_800_000L;
    }

    @Getter
    @Setter
    public static class Cors {

        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}