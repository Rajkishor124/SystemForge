package com.systemforge.backend.architect.orchestrator;

import com.systemforge.backend.architect.dto.AgentStep;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Per-request mutable context (blackboard pattern).
 *
 * <p>Carries the full state of a single agent invocation:
 * user input, conversation history, intermediate reasoning steps,
 * tool outputs, and the final response.
 *
 * <p>This object is NOT thread-safe — it is scoped to a single request.
 */
@Getter
@Setter
public class AgentContext {

    /** Unique trace ID for this request (for log correlation). */
    private final String traceId;

    /** The current user message. */
    private final String userMessage;

    /** Session ID (null for new sessions). */
    private UUID sessionId;

    /** User ID from security context. */
    private UUID userId;

    /** Conversation history (last N messages). */
    private List<String> conversationHistory = new ArrayList<>();

    /** Classified intent. */
    private String intent;

    /** Ordered list of reasoning steps produced during execution. */
    private final List<AgentStep> steps = new ArrayList<>();

    /** Accumulated structured data from decision steps. */
    private final Map<String, Object> stepOutputs = new LinkedHashMap<>();

    /** The final markdown reply to send back. */
    private String finalReply;

    /** Response source: AI, RULE_ENGINE, FALLBACK. */
    private String source = "AI";

    /** Processing start time (epoch ms). */
    private final long startTimeMs;

    public AgentContext(String userMessage) {
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
        this.userMessage = userMessage;
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * Add a completed reasoning step.
     */
    public void addStep(AgentStep step) {
        this.steps.add(step);
    }

    /**
     * Store a step's structured output for downstream steps to consume.
     */
    public void putStepOutput(String key, Object value) {
        this.stepOutputs.put(key, value);
    }

    /**
     * Retrieve a previous step's output.
     */
    @SuppressWarnings("unchecked")
    public <T> T getStepOutput(String key, Class<T> type) {
        return (T) this.stepOutputs.get(key);
    }

    /**
     * Get total elapsed time in ms.
     */
    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Build conversation context string for prompt injection.
     */
    public String getConversationContextString() {
        if (conversationHistory.isEmpty()) return "(No previous context)";
        int start = Math.max(0, conversationHistory.size() - 10);
        return String.join("\n", conversationHistory.subList(start, conversationHistory.size()));
    }
}
