package com.systemforge.backend.architect.maba;

import com.systemforge.backend.common.enums.AgentRole;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * <p><b>Thread safety:</b> Agent outputs are stored in a {@link ConcurrentHashMap}
 * to allow safe parallel writes during Phase 1, 3, and 4. Phase-level warnings
 * are tracked in a {@link CopyOnWriteArrayList}. All other mutable fields are
 * only written from the orchestrator thread (single-writer), so no further
 * synchronization is needed for them.
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
    private volatile String requirementContract;

    /** Thread-safe map of agent outputs. Supports concurrent writes from parallel phases. */
    private final Map<AgentRole, AgentOutput> agentOutputs = new ConcurrentHashMap<>();

    /** Ordered list tracking the execution sequence for metadata reporting. */
    private final List<AgentRole> executionOrder = new CopyOnWriteArrayList<>();

    /** Warnings accumulated during pipeline execution (e.g., degraded agents). */
    private final List<String> warnings = new CopyOnWriteArrayList<>();

    /** The final synthesized System Design Document (Phase 6 output). */
    private volatile String finalDocument;

    /** Pipeline status: RUNNING, COMPLETED, FAILED. */
    private volatile String status = "RUNNING";

    /** If the pipeline failed, the reason. */
    private volatile String failureReason;

    /** The current phase being executed (0-6). */
    private volatile int currentPhase = 0;

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
     * Store an agent's output. Safe to call from parallel threads.
     */
    public void addAgentOutput(AgentRole role, AgentOutput output) {
        this.agentOutputs.put(role, output);
        this.executionOrder.add(role);

        // Track degraded outputs as warnings
        if ("DEGRADED".equals(output.getStatus())) {
            this.warnings.add(role.getRoleName() + " returned a degraded (fallback) response.");
        } else if ("FAILED".equals(output.getStatus())) {
            this.warnings.add(role.getRoleName() + " failed: " + output.getSummary());
        }
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
     * Check if a specific agent has completed (with any status).
     */
    public boolean hasAgentOutput(AgentRole role) {
        return agentOutputs.containsKey(role);
    }

    /**
     * Check if a specific agent has completed successfully (COMPLETED or DEGRADED).
     */
    public boolean hasUsableOutput(AgentRole role) {
        AgentOutput output = agentOutputs.get(role);
        if (output == null) return false;
        return !"FAILED".equals(output.getStatus())
                && output.getContent() != null
                && !output.getContent().isBlank();
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
     * Count agents that completed successfully.
     */
    public long getSuccessfulAgentCount() {
        return agentOutputs.values().stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()) || "DEGRADED".equals(o.getStatus()))
                .count();
    }

    /**
     * Count agents that failed entirely.
     */
    public long getFailedAgentCount() {
        return agentOutputs.values().stream()
                .filter(o -> "FAILED".equals(o.getStatus()))
                .count();
    }

    /**
     * Build a context payload string for a downstream agent.
     *
     * <p>Concatenates all completed agent outputs. If an upstream agent failed,
     * a warning is injected so the downstream agent can compensate.
     */
    public String buildDownstreamContext(AgentRole... roles) {
        StringBuilder sb = new StringBuilder();
        for (AgentRole role : roles) {
            AgentOutput output = agentOutputs.get(role);
            if (output == null) {
                sb.append("═══ ").append(role.getRoleName()).append(" OUTPUT ═══\n");
                sb.append("[NOT AVAILABLE — this agent was not executed or has no output yet.]\n\n");
            } else if ("FAILED".equals(output.getStatus())) {
                sb.append("═══ ").append(role.getRoleName()).append(" OUTPUT ═══\n");
                sb.append("[FAILED — this agent encountered an error: ").append(output.getSummary())
                        .append(". Please compensate with your own best judgment.]\n\n");
            } else {
                sb.append("═══ ").append(role.getRoleName()).append(" OUTPUT ═══\n");
                if (output.isFallback()) {
                    sb.append("[⚠ DEGRADED — this output came from a fallback model. Verify recommendations.]\n");
                }
                sb.append(output.getContent()).append("\n\n");
            }
        }
        return sb.toString();
    }

    // ─── Persistence ──────────────────────────────────────────────────────

    /**
     * Build a structured metadata map for JSON serialization.
     *
     * <p>This returns a plain Map that should be serialized with Jackson
     * by the caller. This avoids manual JSON string building.
     */
    public Map<String, Object> toMetadataMap() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("traceId", traceId);
        meta.put("status", status);
        meta.put("totalDurationMs", getElapsedMs());
        meta.put("totalPromptTokens", getTotalPromptTokens());
        meta.put("totalCompletionTokens", getTotalCompletionTokens());
        meta.put("successfulAgents", getSuccessfulAgentCount());
        meta.put("failedAgents", getFailedAgentCount());

        List<Map<String, Object>> agentList = new ArrayList<>();
        for (AgentRole role : executionOrder) {
            AgentOutput o = agentOutputs.get(role);
            if (o == null) continue;
            // Avoid duplicates if the same role appears twice in executionOrder (shouldn't happen, but defensive)
            if (agentList.stream().anyMatch(m -> role.name().equals(m.get("role")))) continue;

            Map<String, Object> agentMeta = new LinkedHashMap<>();
            agentMeta.put("role", role.name());
            agentMeta.put("roleName", role.getRoleName());
            agentMeta.put("status", o.getStatus());
            agentMeta.put("durationMs", o.getDurationMs());
            agentMeta.put("promptTokens", o.getPromptTokens());
            agentMeta.put("completionTokens", o.getCompletionTokens());
            agentMeta.put("fallback", o.isFallback());
            agentMeta.put("model", o.getModelUsed());
            agentList.add(agentMeta);
        }
        meta.put("agents", agentList);

        if (!warnings.isEmpty()) {
            meta.put("warnings", new ArrayList<>(warnings));
        }
        if (failureReason != null) {
            meta.put("failureReason", failureReason);
        }
        return meta;
    }
}
