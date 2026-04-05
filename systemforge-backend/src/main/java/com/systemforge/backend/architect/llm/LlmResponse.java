package com.systemforge.backend.architect.llm;

import lombok.Builder;
import lombok.Getter;

/**
 * Response wrapper from an LLM call.
 *
 * <p>Carries the content alongside operational metadata
 * (token usage, latency, model ID) for observability.
 */
@Getter
@Builder
public class LlmResponse {

    /** The text content returned by the model. */
    private final String content;

    /** Model identifier (e.g., "gpt-4", "claude-3-opus"). */
    private final String model;

    /** Approximate input token count. */
    private final int promptTokens;

    /** Approximate output token count. */
    private final int completionTokens;

    /** Wall-clock latency in milliseconds. */
    private final long latencyMs;

    /** Whether this response came from a fallback/cache. */
    private final boolean fallback;
}
