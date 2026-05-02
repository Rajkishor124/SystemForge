package com.systemforge.backend.architect.maba;

import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.common.enums.AgentRole;
import com.systemforge.backend.common.event.EventBus;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Multi-Agent Backend Architecture (MABA) Orchestrator.
 *
 * <p>Executes the 7-phase agent pipeline defined in the Enhanced Agent Prompt Suite:
 * <pre>
 *   Phase 0 → Orchestrator (Requirement Contract decomposition)
 *   Phase 1 → RAG Engine, then Requirements Analyst (sequential)
 *   Phase 2 → System Architect
 *   Phase 3 → Database Architect, then API Designer (sequential)
 *   Phase 4 → Scalability Engineer, then Security Engineer (sequential)
 *   Phase 5 → Implementation Planner
 *   Phase 6 → Final Synthesizer
 * </pre>
 *
 * <p>Enterprise-grade design decisions:
 * <ul>
 *   <li>Each agent call uses the full system-prompt from {@link MabaPromptRegistry}</li>
 *   <li>Downstream agents receive upstream outputs via {@link MabaContext#buildDownstreamContext}</li>
 *   <li><b>All agents run sequentially</b> with a configurable inter-call delay to respect
 *       LLM provider rate limits (critical for free-tier API keys)</li>
 *   <li>SSE progress events are emitted at each phase boundary for real-time client updates</li>
 *   <li>Individual agent failures are isolated — the pipeline continues with degraded output</li>
 *   <li>Missing prompts are handled gracefully (agent is skipped, not crashed)</li>
 *   <li>Thread interruption is checked between phases for clean cancellation</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MabaOrchestrator {

    private static final int TOTAL_PHASES = 7;

    /**
     * Delay in milliseconds between sequential agent calls.
     * This prevents overwhelming rate-limited LLM APIs (e.g., Gemini free tier: 15 RPM).
     */
    private static final long INTER_AGENT_DELAY_MS = 4_000; // 4 seconds

    private final LlmClient llmClient;
    private final MabaPromptRegistry promptRegistry;
    private final EventBus eventBus;

    /**
     * Execute the full MABA pipeline.
     *
     * <p>This is the main entry point. Called from the async generation worker
     * in {@code GenerationWorker.executeWithRetries()}.
     *
     * @param context the pipeline context (must have userRequirements set)
     * @return the same context, now populated with all agent outputs and the final document
     */
    public MabaContext execute(MabaContext context) {
        log.info("[MABA:{}] Starting multi-agent pipeline for jobId={}",
                context.getTraceId(), context.getJobId());

        try {
            // Phase 0: Orchestrator — decompose into Requirement Contract
            executePhase(context, 0, "Requirement Decomposition", () -> runOrchestrator(context));

            // Validate: if orchestrator failed, the entire pipeline is degraded but still runs
            if (context.getRequirementContract() == null || context.getRequirementContract().isBlank()) {
                log.warn("[MABA:{}] Orchestrator produced empty Requirement Contract. " +
                        "Downstream agents will use raw user requirements.", context.getTraceId());
                context.setRequirementContract(context.getUserRequirements());
                context.getWarnings().add("Orchestrator produced empty output; using raw requirements.");
            }

            // Phase 1a: RAG Engine
            executePhase(context, 1, "Knowledge Retrieval", () -> runAgent(context, AgentRole.RAG_ENGINE,
                    "Analyze the following requirements and retrieve relevant architectural patterns:\n\n"
                    + context.getRequirementContract()));
            rateLimitDelay();

            // Phase 1b: Requirements Analyst
            executePhase(context, 1, "Requirements Analysis", () -> runAgent(context, AgentRole.REQUIREMENTS_ANALYST,
                    "Analyze the following requirements into a structured specification:\n\n"
                    + context.getUserRequirements() + "\n\n"
                    + "REQUIREMENT CONTRACT:\n" + context.getRequirementContract()));
            rateLimitDelay();

            // Phase 2: System Architect
            executePhase(context, 2, "System Architecture", () -> runAgent(
                    context, AgentRole.SYSTEM_ARCHITECT,
                    "Design the system architecture based on the following inputs:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST, AgentRole.RAG_ENGINE)
            ));
            rateLimitDelay();

            // Phase 3a: DB Architect
            executePhase(context, 3, "Database Design", () -> runAgent(context, AgentRole.DB_DESIGNER,
                    "Design the database architecture:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                            AgentRole.SYSTEM_ARCHITECT, AgentRole.RAG_ENGINE)));
            rateLimitDelay();

            // Phase 3b: API Designer
            executePhase(context, 3, "API Design", () -> runAgent(context, AgentRole.API_DESIGNER,
                    "Design the API contracts:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                            AgentRole.SYSTEM_ARCHITECT)));
            rateLimitDelay();

            // Phase 4a: Scalability Engineer
            executePhase(context, 4, "Scalability Engineering", () -> runAgent(context, AgentRole.SCALABILITY_ENGINEER,
                    "Design the scalability and reliability strategy:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                            AgentRole.SYSTEM_ARCHITECT, AgentRole.DB_DESIGNER)));
            rateLimitDelay();

            // Phase 4b: Security Engineer
            executePhase(context, 4, "Security Engineering", () -> runAgent(context, AgentRole.SECURITY_ENGINEER,
                    "Design the security architecture:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                            AgentRole.SYSTEM_ARCHITECT, AgentRole.API_DESIGNER, AgentRole.RAG_ENGINE)));

            rateLimitDelay();

            // Phase 5: Implementation Planner
            executePhase(context, 5, "Implementation Planning", () -> runAgent(
                    context, AgentRole.IMPLEMENTATION_PLANNER,
                    "Create an implementation roadmap from the following complete design:\n\n"
                    + context.buildDownstreamContext(
                            AgentRole.SYSTEM_ARCHITECT, AgentRole.DB_DESIGNER,
                            AgentRole.API_DESIGNER, AgentRole.SCALABILITY_ENGINEER,
                            AgentRole.SECURITY_ENGINEER)
            ));

            rateLimitDelay();

            // Phase 6: Final Synthesis
            executePhase(context, 6, "Final Synthesis", () -> runFinalSynthesis(context));

            // Determine final status based on agent health
            if (context.getFailedAgentCount() == 0) {
                context.setStatus("COMPLETED");
            } else if (context.getSuccessfulAgentCount() > 0 && context.getFinalDocument() != null) {
                context.setStatus("COMPLETED");
                context.getWarnings().add(context.getFailedAgentCount()
                        + " agent(s) failed but the pipeline produced a result.");
            } else {
                context.setStatus("FAILED");
                context.setFailureReason("All critical agents failed.");
            }

            log.info("[MABA:{}] Pipeline {} in {}ms. Tokens: prompt={}, completion={}, " +
                            "successfulAgents={}, failedAgents={}, warnings={}",
                    context.getTraceId(), context.getStatus(), context.getElapsedMs(),
                    context.getTotalPromptTokens(), context.getTotalCompletionTokens(),
                    context.getSuccessfulAgentCount(), context.getFailedAgentCount(),
                    context.getWarnings().size());

        } catch (Exception e) {
            context.setStatus("FAILED");
            context.setFailureReason(e.getMessage());
            log.error("[MABA:{}] Pipeline failed at phase {}: {}",
                    context.getTraceId(), context.getCurrentPhase(), e.getMessage(), e);
        }

        return context;
    }

    // ─── Phase Executors ──────────────────────────────────────────────────

    private void executePhase(MabaContext context, int phase, String name, Runnable task) {
        checkInterrupted(context);

        context.setCurrentPhase(phase);
        emitProgress(context, name, phase);
        log.info("[MABA:{}] Phase {} — {}", context.getTraceId(), phase, name);

        long start = System.currentTimeMillis();
        task.run();
        long duration = System.currentTimeMillis() - start;

        emitPhaseCompleted(context, name, phase, duration);
        log.info("[MABA:{}] Phase {} completed in {}ms", context.getTraceId(), phase, duration);
    }



    // ─── Agent Runners ────────────────────────────────────────────────────

    /**
     * Phase 0: The Orchestrator produces the Requirement Contract.
     */
    private void runOrchestrator(MabaContext context) {
        if (!promptRegistry.hasPrompt(AgentRole.ORCHESTRATOR)) {
            log.warn("[MABA:{}] Orchestrator prompt not loaded — using raw requirements", context.getTraceId());
            context.setRequirementContract(context.getUserRequirements());
            context.addAgentOutput(AgentRole.ORCHESTRATOR,
                    AgentOutput.skipped(AgentRole.ORCHESTRATOR, "Prompt not loaded"));
            return;
        }

        String systemPrompt = promptRegistry.getPrompt(AgentRole.ORCHESTRATOR);
        String userPrompt = "Parse the following user requirements into a structured Requirement Contract. "
                + "Output ONLY the Requirement Contract section:\n\n"
                + context.getUserRequirements();

        long start = System.currentTimeMillis();
        try {
            LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
            long duration = System.currentTimeMillis() - start;

            context.setRequirementContract(response.getContent());
            context.addAgentOutput(AgentRole.ORCHESTRATOR,
                    AgentOutput.fromLlmResponse(AgentRole.ORCHESTRATOR, response, duration));

            log.info("[MABA:{}] Orchestrator produced Requirement Contract ({}ms, {} tokens, model={})",
                    context.getTraceId(), duration,
                    response.getPromptTokens() + response.getCompletionTokens(),
                    response.getModel());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[MABA:{}] Orchestrator failed: {}", context.getTraceId(), e.getMessage());
            context.addAgentOutput(AgentRole.ORCHESTRATOR,
                    AgentOutput.failed(AgentRole.ORCHESTRATOR, e.getMessage(), duration));
        }
    }

    /**
     * Run a specialist agent with its system prompt and the assembled user context.
     *
     * <p>Failures are isolated — if this agent fails, the pipeline continues.
     */
    private void runAgent(MabaContext context, AgentRole role, String userPrompt) {
        // Guard: check if prompt exists
        if (!promptRegistry.hasPrompt(role)) {
            log.warn("[MABA:{}] No prompt loaded for agent {}, skipping", context.getTraceId(), role.getRoleName());
            context.addAgentOutput(role, AgentOutput.skipped(role, "Prompt not loaded"));
            return;
        }

        String systemPrompt = promptRegistry.getPrompt(role);
        long start = System.currentTimeMillis();

        try {
            LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
            long duration = System.currentTimeMillis() - start;

            AgentOutput output = AgentOutput.fromLlmResponse(role, response, duration);
            context.addAgentOutput(role, output);

            if (output.isUsable()) {
                log.info("[MABA:{}] Agent {} completed ({}ms, {} tokens, model={}, status={})",
                        context.getTraceId(), role.getRoleName(), duration,
                        response.getPromptTokens() + response.getCompletionTokens(),
                        response.getModel(), output.getStatus());
            } else {
                log.warn("[MABA:{}] Agent {} produced unusable output ({}ms, status={})",
                        context.getTraceId(), role.getRoleName(), duration, output.getStatus());
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[MABA:{}] Agent {} failed after {}ms: {}",
                    context.getTraceId(), role.getRoleName(), duration, e.getMessage());
            context.addAgentOutput(role, AgentOutput.failed(role, e.getMessage(), duration));
        }
    }

    /**
     * Phase 6: Final Synthesis — aggregates all agent outputs into one document.
     */
    private void runFinalSynthesis(MabaContext context) {
        if (!promptRegistry.hasPrompt(AgentRole.FINAL_SYNTHESIZER)) {
            log.warn("[MABA:{}] Final synthesizer prompt not loaded", context.getTraceId());
            // Build a basic document from whatever we have
            context.setFinalDocument(buildEmergencyDocument(context));
            context.addAgentOutput(AgentRole.FINAL_SYNTHESIZER,
                    AgentOutput.skipped(AgentRole.FINAL_SYNTHESIZER, "Prompt not loaded"));
            return;
        }

        String systemPrompt = promptRegistry.getPrompt(AgentRole.FINAL_SYNTHESIZER);

        // Build the full context from all prior agents
        String allOutputs = context.buildDownstreamContext(
                AgentRole.REQUIREMENTS_ANALYST,
                AgentRole.SYSTEM_ARCHITECT,
                AgentRole.DB_DESIGNER,
                AgentRole.API_DESIGNER,
                AgentRole.SCALABILITY_ENGINEER,
                AgentRole.SECURITY_ENGINEER,
                AgentRole.IMPLEMENTATION_PLANNER
        );

        String userPrompt = "Synthesize the following agent outputs into a single, unified "
                + "System Design Document. Follow the output format exactly.\n\n"
                + "REQUIREMENT CONTRACT:\n" + context.getRequirementContract() + "\n\n"
                + "AGENT OUTPUTS:\n" + allOutputs;

        long start = System.currentTimeMillis();
        try {
            LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
            long duration = System.currentTimeMillis() - start;

            String finalContent = response.getContent();
            if (finalContent == null || finalContent.isBlank()) {
                log.warn("[MABA:{}] Final synthesizer returned empty content, using emergency document",
                        context.getTraceId());
                finalContent = buildEmergencyDocument(context);
                context.getWarnings().add("Final synthesizer returned empty output; using emergency assembly.");
            }

            context.setFinalDocument(finalContent);
            context.addAgentOutput(AgentRole.FINAL_SYNTHESIZER,
                    AgentOutput.fromLlmResponse(AgentRole.FINAL_SYNTHESIZER, response, duration));

            log.info("[MABA:{}] Final synthesis completed ({}ms, model={})",
                    context.getTraceId(), duration, response.getModel());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[MABA:{}] Final synthesis failed: {}", context.getTraceId(), e.getMessage());

            // Emergency fallback: concatenate whatever we have
            context.setFinalDocument(buildEmergencyDocument(context));
            context.addAgentOutput(AgentRole.FINAL_SYNTHESIZER,
                    AgentOutput.failed(AgentRole.FINAL_SYNTHESIZER, e.getMessage(), duration));
            context.getWarnings().add("Final synthesizer failed; assembled document from raw agent outputs.");
        }
    }

    // ─── Emergency Fallback ───────────────────────────────────────────────

    /**
     * Builds a basic document by concatenating all usable agent outputs.
     * This is the last-resort fallback when the final synthesizer fails.
     */
    private String buildEmergencyDocument(MabaContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("# System Design Document (Auto-assembled)\n\n");
        sb.append("> ⚠️ This document was assembled automatically because the final synthesis agent was unavailable.\n\n");

        sb.append("## Requirements\n\n");
        sb.append(context.getRequirementContract()).append("\n\n");

        AgentRole[] roles = {
                AgentRole.REQUIREMENTS_ANALYST, AgentRole.SYSTEM_ARCHITECT,
                AgentRole.DB_DESIGNER, AgentRole.API_DESIGNER,
                AgentRole.SCALABILITY_ENGINEER, AgentRole.SECURITY_ENGINEER,
                AgentRole.IMPLEMENTATION_PLANNER
        };

        for (AgentRole role : roles) {
            if (context.hasUsableOutput(role)) {
                sb.append("## ").append(role.getRoleName()).append("\n\n");
                sb.append(context.getAgentContent(role)).append("\n\n");
            }
        }

        return sb.toString();
    }

    // ─── SSE Progress ─────────────────────────────────────────────────────

    private void emitProgress(MabaContext context, String stepName, int phase) {
        if (context.getJobId() == null) return;

        try {
            int progressPercent = (int) ((phase / (double) TOTAL_PHASES) * 100);
            eventBus.publish(context.getJobId(),
                    GenerationProgressEvent.stepStarted(stepName, phase + 1, TOTAL_PHASES));
            eventBus.publish(context.getJobId(),
                    GenerationProgressEvent.progress(progressPercent));
        } catch (Exception e) {
            log.debug("[MABA:{}] SSE emit failed (no listener?): {}", context.getTraceId(), e.getMessage());
        }
    }

    private void emitPhaseCompleted(MabaContext context, String stepName, int phase, long durationMs) {
        if (context.getJobId() == null) return;

        try {
            int progressPercent = (int) (((phase + 1) / (double) TOTAL_PHASES) * 100);
            eventBus.publish(context.getJobId(),
                    GenerationProgressEvent.stepCompleted(stepName, phase + 1, TOTAL_PHASES,
                            stepName + " completed", durationMs));
            eventBus.publish(context.getJobId(),
                    GenerationProgressEvent.progress(Math.min(progressPercent, 99)));
        } catch (Exception e) {
            log.debug("[MABA:{}] SSE phase-complete emit failed: {}", context.getTraceId(), e.getMessage());
        }
    }

    // ─── Utility ──────────────────────────────────────────────────────────

    /**
     * Checks if the current thread has been interrupted (e.g., by job cancellation)
     * and throws early rather than starting a new phase.
     */
    private void checkInterrupted(MabaContext context) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Pipeline interrupted before phase " + context.getCurrentPhase());
        }
    }

    /**
     * Introduces a delay between sequential agent calls to respect LLM API rate limits.
     * Gemini free tier allows ~15 RPM, so 4 seconds between calls keeps us well under.
     */
    private void rateLimitDelay() {
        try {
            log.debug("[MABA] Rate-limit delay: waiting {}ms before next agent call", INTER_AGENT_DELAY_MS);
            Thread.sleep(INTER_AGENT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
