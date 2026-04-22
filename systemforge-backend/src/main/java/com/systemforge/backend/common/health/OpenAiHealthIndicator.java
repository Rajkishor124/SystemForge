package com.systemforge.backend.common.health;

import com.systemforge.backend.architect.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for OpenAI / LLM availability.
 *
 * <p>Reports the circuit breaker state and LLM client readiness.
 * When the circuit breaker is OPEN, the health status is DOWN,
 * which triggers alerts in monitoring systems (Grafana, PagerDuty, etc.).
 *
 * <p>Exposed at: {@code GET /actuator/health} → {@code components.openai}
 */
@Component("openai")
@RequiredArgsConstructor
public class OpenAiHealthIndicator implements HealthIndicator {

    private final LlmClient llmClient;

    @Override
    public Health health() {
        boolean available = llmClient.isAvailable();

        if (available) {
            return Health.up()
                    .withDetail("provider", "openai")
                    .withDetail("circuitBreaker", "CLOSED/HALF_OPEN")
                    .withDetail("status", "accepting_requests")
                    .build();
        }

        return Health.down()
                .withDetail("provider", "openai")
                .withDetail("circuitBreaker", "OPEN or client unavailable")
                .withDetail("status", "rejecting_requests")
                .withDetail("fallback", "rule-based engine active")
                .build();
    }
}
