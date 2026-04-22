package com.systemforge.backend.architect.maba;

import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.common.enums.AgentRole;
import lombok.Builder;
import lombok.Getter;

/**
 * Output of a single MABA agent execution.
 *
 * <p>Contains the agent's full markdown output, the role that produced it,
 * token usage for cost tracking, and execution timing for observability.
 */
@Getter
@Builder
public class AgentOutput {

    /** Which agent produced this output. */
    private final AgentRole role;

    /** The agent's full markdown output (to be fed to downstream agents). */
    private final String content;

    /** Structured summary for the agent audit log. */
    private final String summary;

    /** Status: COMPLETED, FAILED, SKIPPED. */
    private final String status;

    /** Approximate input tokens consumed. */
    private final int promptTokens;

    /** Approximate output tokens produced. */
    private final int completionTokens;

    /** Wall-clock execution time in milliseconds. */
    private final long durationMs;

    /** Whether this output came from a fallback. */
    private final boolean fallback;

    /**
     * Convenience factory from a raw {@link LlmResponse}.
     */
    public static AgentOutput fromLlmResponse(AgentRole role, LlmResponse response, long durationMs) {
        return AgentOutput.builder()
                .role(role)
                .content(response.getContent())
                .summary(truncate(response.getContent(), 200))
                .status(response.isFallback() ? "DEGRADED" : "COMPLETED")
                .promptTokens(response.getPromptTokens())
                .completionTokens(response.getCompletionTokens())
                .durationMs(durationMs)
                .fallback(response.isFallback())
                .build();
    }

    /**
     * Factory for a failed agent.
     */
    public static AgentOutput failed(AgentRole role, String reason, long durationMs) {
        return AgentOutput.builder()
                .role(role)
                .content("")
                .summary("FAILED: " + reason)
                .status("FAILED")
                .promptTokens(0)
                .completionTokens(0)
                .durationMs(durationMs)
                .fallback(false)
                .build();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
