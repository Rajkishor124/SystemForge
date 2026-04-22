package com.systemforge.backend.architect.maba;

import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.common.enums.AgentRole;
import com.systemforge.backend.common.sse.SseEmitterRegistry;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-Agent Backend Architecture (MABA) Orchestrator.
 *
 * <p>Executes the 7-phase agent pipeline defined in the Enhanced Agent Prompt Suite:
 * <pre>
 *   Phase 0 → Orchestrator (Requirement Contract decomposition)
 *   Phase 1 → RAG Engine + Requirements Analyst (parallel)
 *   Phase 2 → System Architect
 *   Phase 3 → Database Architect + API Designer (parallel)
 *   Phase 4 → Scalability Engineer + Security Engineer (parallel)
 *   Phase 5 → Implementation Planner
 *   Phase 6 → Final Synthesizer
 * </pre>
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Each agent call uses the full system-prompt from {@link MabaPromptRegistry}</li>
 *   <li>Downstream agents receive upstream outputs via {@link MabaContext#buildDownstreamContext}</li>
 *   <li>Parallel phases use {@link CompletableFuture} with a dedicated virtual thread executor</li>
 *   <li>SSE progress events are emitted at each phase boundary for real-time client updates</li>
 *   <li>Individual agent failures are isolated — the pipeline continues with degraded output</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MabaOrchestrator {

    private static final int TOTAL_PHASES = 7;

    private final LlmClient llmClient;
    private final MabaPromptRegistry promptRegistry;
    private final SseEmitterRegistry sseRegistry;

    // Virtual threads for parallel agent execution (Java 21+)
    private final ExecutorService parallelExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Execute the full MABA pipeline.
     *
     * <p>This is the main entry point. Called from the async generation worker
     * in {@code SystemServiceImpl.executeGenerationAsync()}.
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

            // Phase 1: RAG + Requirements Analyst (parallel)
            executeParallelPhase(context, 1, "Knowledge Retrieval & Requirements",
                    () -> runAgent(context, AgentRole.RAG_ENGINE,
                            "Analyze the following requirements and retrieve relevant architectural patterns:\n\n"
                            + context.getRequirementContract()),
                    () -> runAgent(context, AgentRole.REQUIREMENTS_ANALYST,
                            "Analyze the following requirements into a structured specification:\n\n"
                            + context.getUserRequirements() + "\n\n"
                            + "REQUIREMENT CONTRACT:\n" + context.getRequirementContract())
            );

            // Phase 2: System Architect
            executePhase(context, 2, "System Architecture", () -> runAgent(
                    context, AgentRole.SYSTEM_ARCHITECT,
                    "Design the system architecture based on the following inputs:\n\n"
                    + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST, AgentRole.RAG_ENGINE)
            ));

            // Phase 3: DB Architect + API Designer (parallel)
            executeParallelPhase(context, 3, "Data & API Design",
                    () -> runAgent(context, AgentRole.DB_DESIGNER,
                            "Design the database architecture:\n\n"
                            + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                                    AgentRole.SYSTEM_ARCHITECT, AgentRole.RAG_ENGINE)),
                    () -> runAgent(context, AgentRole.API_DESIGNER,
                            "Design the API contracts:\n\n"
                            + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                                    AgentRole.SYSTEM_ARCHITECT))
            );

            // Phase 4: Scalability + Security (parallel)
            executeParallelPhase(context, 4, "Scalability & Security",
                    () -> runAgent(context, AgentRole.SCALABILITY_ENGINEER,
                            "Design the scalability and reliability strategy:\n\n"
                            + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                                    AgentRole.SYSTEM_ARCHITECT, AgentRole.DB_DESIGNER)),
                    () -> runAgent(context, AgentRole.SECURITY_ENGINEER,
                            "Design the security architecture:\n\n"
                            + context.buildDownstreamContext(AgentRole.REQUIREMENTS_ANALYST,
                                    AgentRole.SYSTEM_ARCHITECT, AgentRole.API_DESIGNER, AgentRole.RAG_ENGINE))
            );

            // Phase 5: Implementation Planner
            executePhase(context, 5, "Implementation Planning", () -> runAgent(
                    context, AgentRole.IMPLEMENTATION_PLANNER,
                    "Create an implementation roadmap from the following complete design:\n\n"
                    + context.buildDownstreamContext(
                            AgentRole.SYSTEM_ARCHITECT, AgentRole.DB_DESIGNER,
                            AgentRole.API_DESIGNER, AgentRole.SCALABILITY_ENGINEER,
                            AgentRole.SECURITY_ENGINEER)
            ));

            // Phase 6: Final Synthesis
            executePhase(context, 6, "Final Synthesis", () -> runFinalSynthesis(context));

            context.setStatus("COMPLETED");
            log.info("[MABA:{}] Pipeline completed in {}ms. Tokens: prompt={}, completion={}",
                    context.getTraceId(), context.getElapsedMs(),
                    context.getTotalPromptTokens(), context.getTotalCompletionTokens());

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
        context.setCurrentPhase(phase);
        emitProgress(context, name, phase);
        log.info("[MABA:{}] Phase {} — {}", context.getTraceId(), phase, name);

        long start = System.currentTimeMillis();
        task.run();
        long duration = System.currentTimeMillis() - start;

        log.info("[MABA:{}] Phase {} completed in {}ms", context.getTraceId(), phase, duration);
    }

    private void executeParallelPhase(MabaContext context, int phase, String name,
                                       Runnable taskA, Runnable taskB) {
        context.setCurrentPhase(phase);
        emitProgress(context, name, phase);
        log.info("[MABA:{}] Phase {} — {} (parallel)", context.getTraceId(), phase, name);

        long start = System.currentTimeMillis();

        CompletableFuture<Void> futureA = CompletableFuture.runAsync(taskA, parallelExecutor);
        CompletableFuture<Void> futureB = CompletableFuture.runAsync(taskB, parallelExecutor);

        // Wait for both to complete
        CompletableFuture.allOf(futureA, futureB).join();

        long duration = System.currentTimeMillis() - start;
        log.info("[MABA:{}] Phase {} (parallel) completed in {}ms", context.getTraceId(), phase, duration);
    }

    // ─── Agent Runners ────────────────────────────────────────────────────

    /**
     * Phase 0: The Orchestrator produces the Requirement Contract.
     */
    private void runOrchestrator(MabaContext context) {
        String systemPrompt = promptRegistry.getPrompt(AgentRole.ORCHESTRATOR);
        String userPrompt = "Parse the following user requirements into a structured Requirement Contract. "
                + "Output ONLY the Requirement Contract section:\n\n"
                + context.getUserRequirements();

        long start = System.currentTimeMillis();
        LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
        long duration = System.currentTimeMillis() - start;

        context.setRequirementContract(response.getContent());
        context.addAgentOutput(AgentRole.ORCHESTRATOR,
                AgentOutput.fromLlmResponse(AgentRole.ORCHESTRATOR, response, duration));

        log.info("[MABA:{}] Orchestrator produced Requirement Contract ({}ms, {} tokens)",
                context.getTraceId(), duration,
                response.getPromptTokens() + response.getCompletionTokens());
    }

    /**
     * Run a specialist agent with its system prompt and the assembled user context.
     */
    private void runAgent(MabaContext context, AgentRole role, String userPrompt) {
        String systemPrompt = promptRegistry.getPrompt(role);
        long start = System.currentTimeMillis();

        try {
            LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
            long duration = System.currentTimeMillis() - start;

            AgentOutput output = AgentOutput.fromLlmResponse(role, response, duration);

            // Synchronized since parallel phases write to the shared context
            synchronized (context) {
                context.addAgentOutput(role, output);
            }

            log.info("[MABA:{}] Agent {} completed ({}ms, {} tokens)",
                    context.getTraceId(), role.getRoleName(), duration,
                    response.getPromptTokens() + response.getCompletionTokens());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[MABA:{}] Agent {} failed: {}", context.getTraceId(), role.getRoleName(), e.getMessage());

            synchronized (context) {
                context.addAgentOutput(role, AgentOutput.failed(role, e.getMessage(), duration));
            }
        }
    }

    /**
     * Phase 6: Final Synthesis — aggregates all agent outputs into one document.
     */
    private void runFinalSynthesis(MabaContext context) {
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
        LlmResponse response = llmClient.complete(systemPrompt, userPrompt);
        long duration = System.currentTimeMillis() - start;

        context.setFinalDocument(response.getContent());
        context.addAgentOutput(AgentRole.FINAL_SYNTHESIZER,
                AgentOutput.fromLlmResponse(AgentRole.FINAL_SYNTHESIZER, response, duration));

        log.info("[MABA:{}] Final synthesis completed ({}ms)", context.getTraceId(), duration);
    }

    // ─── SSE Progress ─────────────────────────────────────────────────────

    private void emitProgress(MabaContext context, String stepName, int phase) {
        if (context.getJobId() == null) return;

        try {
            int progressPercent = (int) ((phase / (double) TOTAL_PHASES) * 100);
            sseRegistry.send(context.getJobId(),
                    GenerationProgressEvent.stepStarted(stepName, phase + 1, TOTAL_PHASES));
            sseRegistry.send(context.getJobId(),
                    GenerationProgressEvent.progress(progressPercent));
        } catch (Exception e) {
            log.debug("[MABA:{}] SSE emit failed (no listener?): {}", context.getTraceId(), e.getMessage());
        }
    }
}
