package com.systemforge.backend.architect.llm.impl;

import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resilient decorator around any {@link LlmClient} implementation.
 *
 * <p>Applies Resilience4j patterns in the following order:
 * <ol>
 *   <li><b>Retry</b> — 3 attempts with exponential backoff (2s → 4s → 8s)
 *       for transient network failures</li>
 *   <li><b>Circuit Breaker</b> — opens after 50% failure rate (min 5 calls),
 *       stays open for 60s, then allows 3 probe calls in half-open</li>
 * </ol>
 *
 * <p>Failure classification:
 * <ul>
 *   <li><b>Counted as failure:</b> Timeouts, 5xx/network errors, IOException</li>
 *   <li><b>NOT counted:</b> Business/validation errors (IllegalArgumentException, etc.)</li>
 * </ul>
 *
 * <p>Fallback: returns a {@link LlmResponse} with {@code fallback=true} and
 * a helpful message explaining the system is using rule-based recommendations.
 */
@Slf4j
public class ResilientLlmClient implements LlmClient {

    /** HTTP status codes that should trigger retries. */
    private static final Set<String> RETRYABLE_STATUS_PATTERNS = Set.of(
            "429", "500", "502", "503", "504", "520", "522", "524"
    );

    private final LlmClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientLlmClient(LlmClient delegate,
                              CircuitBreakerRegistry cbRegistry,
                              RetryRegistry retryRegistry) {
        this.delegate = delegate;
        this.circuitBreaker = cbRegistry.circuitBreaker("openai");
        this.retry = retryRegistry.retry("openai");

        // Log state transitions for observability
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn(
                        "[CIRCUIT_BREAKER] OpenAI state transition: {} → {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> log.warn(
                        "[CIRCUIT_BREAKER] Call rejected — circuit is OPEN"))
                .onError(event -> log.warn(
                        "[CIRCUIT_BREAKER] Recorded failure: {} (duration={}ms)",
                        event.getThrowable().getClass().getSimpleName(),
                        event.getElapsedDuration().toMillis()))
                .onSuccess(event -> log.debug(
                        "[CIRCUIT_BREAKER] Successful call (duration={}ms)",
                        event.getElapsedDuration().toMillis()));

        retry.getEventPublisher()
                .onRetry(event -> log.info(
                        "[RETRY] Attempt #{} for OpenAI call, waiting {}ms before next. Cause: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getWaitInterval().toMillis(),
                        event.getLastThrowable() != null
                                ? event.getLastThrowable().getClass().getSimpleName()
                                        + ": " + event.getLastThrowable().getMessage()
                                : "unknown"));
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt) {
        Supplier<LlmResponse> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, () -> delegate.complete(systemPrompt, userPrompt))
        );

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("[RESILIENT_LLM] Circuit OPEN — returning fallback for complete()");
            return buildFallbackResponse();
        } catch (Exception e) {
            if (isRetryableFailure(e)) {
                log.error("[RESILIENT_LLM] All retries exhausted for complete(): {} - {}",
                        e.getClass().getSimpleName(), e.getMessage());
                return buildFallbackResponse();
            }
            // Non-retryable (business error) — propagate
            throw e;
        }
    }

    @Override
    public <T> T completeStructured(String systemPrompt, String userPrompt, Class<T> responseType) {
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry,
                        () -> delegate.completeStructured(systemPrompt, userPrompt, responseType))
        );

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("[RESILIENT_LLM] Circuit OPEN — structured call rejected for type: {}",
                    responseType.getSimpleName());
            // Structured calls cannot return a meaningful fallback of type T
            throw new RuntimeException(
                    "AI service temporarily unavailable (circuit breaker open). " +
                    "Please try again in 60 seconds.", e);
        } catch (Exception e) {
            if (isRetryableFailure(e)) {
                log.error("[RESILIENT_LLM] All retries exhausted for completeStructured({}): {} - {}",
                        responseType.getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
                throw new RuntimeException(
                        "AI service temporarily unavailable after retries. " +
                        "Please try again later.", e);
            }
            throw e;
        }
    }

    @Override
    public boolean isAvailable() {
        // Available if delegate is ready AND circuit is not fully OPEN
        if (!delegate.isAvailable()) return false;

        CircuitBreaker.State state = circuitBreaker.getState();
        return state != CircuitBreaker.State.OPEN &&
               state != CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * Returns the current state of the circuit breaker.
     * Useful for health checks and status endpoints.
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Returns the circuit breaker metrics for observability.
     */
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }

    // ─── Fallback ─────────────────────────────────────────────────────────

    private LlmResponse buildFallbackResponse() {
        return LlmResponse.builder()
                .content("⚠️ **AI service is temporarily unavailable.**\n\n" +
                         "The system is currently using rule-based recommendations. " +
                         "Your request has been processed with the built-in knowledge base.\n\n" +
                         "The AI service will automatically recover. Please try again in a few minutes " +
                         "for full AI-powered analysis.")
                .model("fallback-rule-engine")
                .promptTokens(0)
                .completionTokens(0)
                .latencyMs(0)
                .fallback(true)
                .build();
    }

    /**
     * Determines if a failure should be retried.
     *
     * <p>Uses a structured approach:
     * <ol>
     *   <li>Check the exception type hierarchy (SocketTimeout, Connect, IO, HttpTimeout)</li>
     *   <li>Check the message for known HTTP status codes (429, 5xx)</li>
     *   <li>Check for rate-limit / timeout keywords in the message</li>
     * </ol>
     *
     * <p>Business errors (bad input, validation) are NOT retryable.
     */
    private boolean isRetryableFailure(Throwable t) {
        Throwable root = getRootCause(t);

        // 1. Exception type check
        if (root instanceof SocketTimeoutException
                || root instanceof ConnectException
                || root instanceof HttpTimeoutException
                || root instanceof IOException) {
            return true;
        }

        // 2. Message-based status code check
        String message = root.getMessage();
        if (message == null) return false;

        for (String code : RETRYABLE_STATUS_PATTERNS) {
            if (message.contains(code)) {
                return true;
            }
        }

        // 3. Keyword check for common transient error patterns
        String lower = message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("rate limit")
                || lower.contains("rate_limit")
                || lower.contains("too many requests")
                || lower.contains("connection reset")
                || lower.contains("connection refused")
                || lower.contains("server error")
                || lower.contains("temporarily unavailable");
    }

    private Throwable getRootCause(Throwable t) {
        Throwable cause = t;
        int depth = 0;
        // Guard against circular cause chains
        while (cause.getCause() != null && cause.getCause() != cause && depth < 10) {
            cause = cause.getCause();
            depth++;
        }
        return cause;
    }
}
