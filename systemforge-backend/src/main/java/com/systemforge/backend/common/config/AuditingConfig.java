package com.systemforge.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing Configuration.
 *
 * <p>Enables automatic population of:
 * <ul>
 *   <li>{@code createdAt} / {@code updatedAt} — via Spring Data's {@code @CreatedDate} / {@code @LastModifiedDate}</li>
 *   <li>{@code createdBy} / {@code updatedBy} — via {@link AuditorAware} resolving the current principal</li>
 * </ul>
 *
 * <p>The auditor is resolved from the Spring Security context. During system/anonymous
 * operations, it falls back to "SYSTEM".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    private static final String SYSTEM_AUDITOR = "SYSTEM";

    /**
     * Resolves the current authenticated principal's identifier.
     *
     * <p>In Phase 1, this returns the username from the SecurityContext.
     * In Phase 2, once JWT is fully integrated, this should return the userId (UUID string)
     * extracted from the JWT claims for a more stable audit trail.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of(SYSTEM_AUDITOR);
            }

            return Optional.ofNullable(authentication.getName())
                    .filter(name -> !name.isBlank())
                    .or(() -> Optional.of(SYSTEM_AUDITOR));
        };
    }
}