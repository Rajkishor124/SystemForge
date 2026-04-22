package com.systemforge.backend.architect.maba;

import com.systemforge.backend.common.enums.AgentRole;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Mutable context for a MABA pipeline execution (blackboard pattern).
 *
 * <p>Carries the full state across all 7 phases:
 * <ol>
 *   <li>The user's original requirements</li>
 *   <li>The Requirement Contract (produced by Orchestrator)</li>
 *   <li>Each agent's output (keyed by {@link AgentRole})</li>
 *   <li>The final synthesized System Design Document</li>
 *   <li>Timing, token usage, and error tracking for observability</li>
 * </ol>
 *
 * <p>This object is NOT thread-safe. When agents run in parallel (Phase 3/4),
 * their outputs are collected via CompletableFuture and merged sequentially
 * into this context after both complete.
 */
@Getter
@Setter
public class MabaContext {

    /** Unique trace ID for log correlation across all agent calls. */
    private final String traceId;

    /** The user's original raw requirements. */
    private final String userRequirements;

    /** User ID (for job tracking and audit). */
    private UUID userId;

    /** Job ID (links to GenerationJob entity). */
    private UUID jobId;

    /** The Requirement Contract produced by the Orchestrator in Phase 0. */
    private String requirementContract;

    /** Ordered map of agent outputs. Insertion order = execution order. */
    private final Map<AgentRole, AgentOutput> agentOutputs = new LinkedHashMap<>();

    /** The final synthesized System Design Document (Phase 6 output). */
    private String finalDocument;

    /** Pipeline status: RUNNING, COMPLETED, FAILED. */
    private String status = "RUNNING";

    /** If the pipeline failed, the reason. */
    private String failureReason;

    /** The current phase being executed (0-6). */
    private int currentPhase = 0;

    /** Pipeline start time (epoch ms). */
    private final long startTimeMs;

    // ─── Constructors ─────────────────────────────────────────────────────

    public MabaContext(String userRequirements) {
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
        this.userRequirements = userRequirements;
        this.startTimeMs = System.currentTimeMillis();
    }

    // ─── Agent Output Management ──────────────────────────────────────────

    /**
     * Store an agent's output. Called after each successful agent execution.
     */
    public void addAgentOutput(AgentRole role, AgentOutput output) {
        this.agentOutputs.put(role, output);
    }

    /**
     * Get a specific agent's output (for downstream agent context injection).
     */
    public AgentOutput getAgentOutput(AgentRole role) {
        return this.agentOutputs.get(role);
    }

    /**
     * Get the content string from a specific agent's output, or empty string.
     */
    public String getAgentContent(AgentRole role) {
        AgentOutput output = agentOutputs.get(role);
        return output != null ? output.getContent() : "";
    }

    /**
     * Check if a specific agent has completed.
     */
    public boolean hasAgentOutput(AgentRole role) {
        return agentOutputs.containsKey(role);
    }

    // ─── Observability ────────────────────────────────────────────────────

    /**
     * Total elapsed time since pipeline start.
     */
    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Total tokens consumed across all agents.
     */
    public int getTotalPromptTokens() {
        return agentOutputs.values().stream().mapToInt(AgentOutput::getPromptTokens).sum();
    }

    /**
     * Total tokens produced across all agents.
     */
    public int getTotalCompletionTokens() {
        return agentOutputs.values().stream().mapToInt(AgentOutput::getCompletionTokens).sum();
    }

    /**
     * Build a context payload string for a downstream agent.
     * Concatenates all completed agent outputs in execution order.
     */
    public String buildDownstreamContext(AgentRole... roles) {
        StringBuilder sb = new StringBuilder();
        for (AgentRole role : roles) {
            AgentOutput output = agentOutputs.get(role);
            if (output != null && !"FAILED".equals(output.getStatus())) {
                sb.append("═══ ").append(role.getRoleName()).append(" OUTPUT ═══\n");
                sb.append(output.getContent()).append("\n\n");
            }
        }
        return sb.toString();
    }

    // ─── Persistence ──────────────────────────────────────────────────────

    /**
     * Serialize pipeline execution metadata to a JSON string
     * for storage in {@code generation_jobs.maba_metadata}.
     *
     * <p>Manually constructed to avoid a Jackson dependency in this DTO layer.
     */
    public String toMetadataJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"traceId\":\"").append(traceId).append("\",");
        sb.append("\"status\":\"").append(status).append("\",");
        sb.append("\"totalDurationMs\":").append(getElapsedMs()).append(",");
        sb.append("\"totalPromptTokens\":").append(getTotalPromptTokens()).append(",");
        sb.append("\"totalCompletionTokens\":").append(getTotalCompletionTokens()).append(",");
        sb.append("\"agents\":[");

        boolean first = true;
        for (Map.Entry<AgentRole, AgentOutput> entry : agentOutputs.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            AgentOutput o = entry.getValue();
            sb.append("{");
            sb.append("\"role\":\"").append(entry.getKey().name()).append("\",");
            sb.append("\"roleName\":\"").append(escapeJson(entry.getKey().getRoleName())).append("\",");
            sb.append("\"status\":\"").append(o.getStatus()).append("\",");
            sb.append("\"durationMs\":").append(o.getDurationMs()).append(",");
            sb.append("\"promptTokens\":").append(o.getPromptTokens()).append(",");
            sb.append("\"completionTokens\":").append(o.getCompletionTokens()).append(",");
            sb.append("\"fallback\":").append(o.isFallback());
            sb.append("}");
        }

        sb.append("]");
        if (failureReason != null) {
            sb.append(",\"failureReason\":\"").append(escapeJson(failureReason)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
