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
 *
 * <p>Output validation: the {@link #isUsable()} method checks whether the
 * content is non-null and non-blank, providing a simple quality gate for
 * downstream agents.
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

    /** Status: COMPLETED, DEGRADED, FAILED, SKIPPED. */
    private final String status;

    /** Approximate input tokens consumed. */
    private final int promptTokens;

    /** Approximate output tokens produced. */
    private final int completionTokens;

    /** Wall-clock execution time in milliseconds. */
    private final long durationMs;

    /** Whether this output came from a fallback. */
    private final boolean fallback;

    /** The model ID that produced this output (e.g., "gpt-4o-mini", "fallback-rule-engine"). */
    private final String modelUsed;

    /**
     * Convenience factory from a raw {@link LlmResponse}.
     *
     * <p>Performs basic output validation: if the LLM returned empty/null content,
     * the status is downgraded to DEGRADED.
     */
    public static AgentOutput fromLlmResponse(AgentRole role, LlmResponse response, long durationMs) {
        String content = response.getContent();
        boolean contentEmpty = content == null || content.isBlank();
        String effectiveStatus;

        if (response.isFallback()) {
            effectiveStatus = "DEGRADED";
        } else if (contentEmpty) {
            effectiveStatus = "DEGRADED";
        } else {
            effectiveStatus = "COMPLETED";
        }

        return AgentOutput.builder()
                .role(role)
                .content(content != null ? content : "")
                .summary(truncate(content, 200))
                .status(effectiveStatus)
                .promptTokens(response.getPromptTokens())
                .completionTokens(response.getCompletionTokens())
                .durationMs(durationMs)
                .fallback(response.isFallback())
                .modelUsed(response.getModel())
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
                .modelUsed("none")
                .build();
    }

    /**
     * Factory for a skipped agent (e.g., when prompt is missing).
     */
    public static AgentOutput skipped(AgentRole role, String reason) {
        return AgentOutput.builder()
                .role(role)
                .content("")
                .summary("SKIPPED: " + reason)
                .status("SKIPPED")
                .promptTokens(0)
                .completionTokens(0)
                .durationMs(0)
                .fallback(false)
                .modelUsed("none")
                .build();
    }

    /**
     * Checks whether this output is usable by downstream agents.
     *
     * @return true if content is non-null, non-blank, and status is not FAILED
     */
    public boolean isUsable() {
        return !"FAILED".equals(status)
                && !"SKIPPED".equals(status)
                && content != null
                && !content.isBlank();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
