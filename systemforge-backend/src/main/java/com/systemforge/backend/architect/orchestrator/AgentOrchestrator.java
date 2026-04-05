package com.systemforge.backend.architect.orchestrator;

import com.systemforge.backend.architect.dto.AgentStep;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.architect.prompts.PromptRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Core agent loop — the brain of the AI Architect.
 *
 * <p>Execution flow:
 * <ol>
 *     <li>Classify user intent</li>
 *     <li>Route to appropriate handler (design / question / followup)</li>
 *     <li>For design requests → run DecisionPipeline (Phase 2)</li>
 *     <li>For questions → direct LLM with system architect persona</li>
 *     <li>Format and return structured response</li>
 * </ol>
 *
 * <p>Falls back to rule-based responses when LLM is unavailable.
 */
@Component
@Slf4j
public class AgentOrchestrator {

    private final PromptRegistry promptRegistry;

    @Autowired(required = false)
    private LlmClient llmClient;

    public AgentOrchestrator(PromptRegistry promptRegistry) {
        this.promptRegistry = promptRegistry;
    }

    /**
     * Execute the full agent pipeline for a given context.
     *
     * @param context mutable per-request context
     */
    public void execute(AgentContext context) {
        String traceId = context.getTraceId();
        log.info("[ARCHITECT:{}] Starting agent execution for message: {}...",
                traceId, truncate(context.getUserMessage(), 80));

        try {
            // Step 1: Classify intent
            String intent = classifyIntent(context);
            context.setIntent(intent);
            log.info("[ARCHITECT:{}] Classified intent: {}", traceId, intent);

            // Step 2: Route by intent
            switch (intent) {
                case "SYSTEM_DESIGN", "ANALYSIS" -> handleDesignRequest(context);
                case "CODE_REQUEST" -> handleCodeRequest(context);
                case "GREETING" -> handleGreeting(context);
                default -> handleQuestion(context);
            }

            log.info("[ARCHITECT:{}] Execution complete. Source={}, Steps={}, TotalMs={}",
                    traceId, context.getSource(), context.getSteps().size(), context.getElapsedMs());

        } catch (Exception e) {
            log.error("[ARCHITECT:{}] Pipeline failed: {}", traceId, e.getMessage(), e);
            handleFallback(context, e);
        }
    }

    // ─── Intent Classification ─────────────────────────────────────────────

    private String classifyIntent(AgentContext context) {
        if (llmClient == null || !llmClient.isAvailable()) {
            return classifyIntentByRules(context.getUserMessage());
        }

        long startTime = System.currentTimeMillis();
        try {
            String prompt = promptRegistry.get("intent_classifier").render(Map.of(
                    "message", context.getUserMessage(),
                    "context", context.getConversationContextString()
            ));

            LlmResponse response = llmClient.complete(
                    "You are a message classifier. Respond with exactly one word.",
                    prompt
            );

            String intent = response.getContent().trim().toUpperCase().replace("\"", "");

            context.addStep(AgentStep.builder()
                    .name("Intent Classification")
                    .order(0)
                    .status("COMPLETED")
                    .output("Detected intent: " + intent)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build());

            // Validate it's a known intent
            return switch (intent) {
                case "SYSTEM_DESIGN", "ANALYSIS", "QUESTION",
                     "CODE_REQUEST", "FOLLOWUP", "GREETING" -> intent;
                default -> "QUESTION";
            };

        } catch (Exception e) {
            log.warn("[ARCHITECT:{}] LLM intent classification failed, using rules: {}",
                    context.getTraceId(), e.getMessage());
            return classifyIntentByRules(context.getUserMessage());
        }
    }

    private String classifyIntentByRules(String message) {
        String lower = message.toLowerCase();
        if (lower.matches(".*\\b(design|build|create|architect|system for)\\b.*")) return "SYSTEM_DESIGN";
        if (lower.matches(".*\\b(analyze|review|improve|optimize|audit)\\b.*")) return "ANALYSIS";
        if (lower.matches(".*\\b(generate|code|implement|spring boot|boilerplate)\\b.*")) return "CODE_REQUEST";
        if (lower.matches(".*\\b(hi|hello|hey|thanks|thank you)\\b.*")) return "GREETING";
        return "QUESTION";
    }

    // ─── Design Request Handler ────────────────────────────────────────────

    private void handleDesignRequest(AgentContext context) {
        if (llmClient == null || !llmClient.isAvailable()) {
            handleFallback(context, null);
            return;
        }

        long startTime = System.currentTimeMillis();

        String systemPrompt = promptRegistry.get("system_architect").render();
        String userPrompt = buildDesignPrompt(context);

        LlmResponse response = llmClient.complete(systemPrompt, userPrompt);

        context.setFinalReply(response.getContent());
        context.setSource("AI");
        context.addStep(AgentStep.builder()
                .name("Architecture Design")
                .order(1)
                .status("COMPLETED")
                .output("Generated comprehensive architecture design")
                .durationMs(System.currentTimeMillis() - startTime)
                .build());
    }

    private String buildDesignPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Design a production-grade backend system based on this request:\n\n");
        sb.append("USER REQUEST: ").append(context.getUserMessage()).append("\n\n");

        if (!context.getConversationHistory().isEmpty()) {
            sb.append("PREVIOUS CONTEXT:\n").append(context.getConversationContextString()).append("\n\n");
        }

        sb.append("""
                RESPOND WITH THIS STRUCTURE:
                
                ## Architecture Overview
                (High-level description of the system)
                
                ## Services
                (List each microservice/module with its responsibility)
                
                ## Database Design
                (Database choices with justification)
                
                ## API Endpoints
                (Key API routes)
                
                ## Scaling Strategy
                (How to scale this system)
                
                ## Tradeoffs
                (Key tradeoffs and alternatives considered)
                
                ## Next Steps
                (Actionable recommendations)
                """);

        return sb.toString();
    }

    // ─── Question Handler ──────────────────────────────────────────────────

    private void handleQuestion(AgentContext context) {
        if (llmClient == null || !llmClient.isAvailable()) {
            handleFallback(context, null);
            return;
        }

        long startTime = System.currentTimeMillis();

        String systemPrompt = promptRegistry.get("system_architect").render();

        StringBuilder userPrompt = new StringBuilder();
        if (!context.getConversationHistory().isEmpty()) {
            userPrompt.append("Previous conversation:\n")
                    .append(context.getConversationContextString())
                    .append("\n\n");
        }
        userPrompt.append("Question: ").append(context.getUserMessage());

        LlmResponse response = llmClient.complete(systemPrompt, userPrompt.toString());

        context.setFinalReply(response.getContent());
        context.setSource("AI");
        context.addStep(AgentStep.builder()
                .name("Question Answering")
                .order(1)
                .status("COMPLETED")
                .output("Answered technical question")
                .durationMs(System.currentTimeMillis() - startTime)
                .build());
    }

    // ─── Code Request Handler ──────────────────────────────────────────────

    private void handleCodeRequest(AgentContext context) {
        // MVP: treat code requests as questions with code emphasis
        if (llmClient == null || !llmClient.isAvailable()) {
            handleFallback(context, null);
            return;
        }

        long startTime = System.currentTimeMillis();

        String systemPrompt = promptRegistry.get("system_architect").render()
                + "\n\nIMPORTANT: Include working code examples in your response. "
                + "Use Spring Boot 3.x, Java 21, and production-grade patterns.";

        LlmResponse response = llmClient.complete(systemPrompt,
                "Generate production code for: " + context.getUserMessage());

        context.setFinalReply(response.getContent());
        context.setSource("AI");
        context.addStep(AgentStep.builder()
                .name("Code Generation")
                .order(1)
                .status("COMPLETED")
                .output("Generated code implementation")
                .durationMs(System.currentTimeMillis() - startTime)
                .build());
    }

    // ─── Greeting Handler ──────────────────────────────────────────────────

    private void handleGreeting(AgentContext context) {
        context.setFinalReply("""
                👋 Hello! I'm **SystemForge AI Architect** — your senior backend systems design partner.

                I can help you with:
                - 🏗️ **System Design** — Design full backend architectures from requirements
                - 🔍 **Architecture Analysis** — Review and improve existing designs
                - 💻 **Code Generation** — Generate Spring Boot service code
                - 📊 **Scaling Strategy** — Plan for growth and high availability
                - ⚖️ **Tradeoff Analysis** — Compare technologies and patterns

                **Try asking something like:**
                > *"Design a backend for a ride-sharing app with 1M users"*
                > *"What database should I use for a real-time analytics platform?"*
                > *"How should I handle authentication for a mobile-first SaaS app?"*
                """);
        context.setSource("RULE_ENGINE");
    }

    // ─── Fallback Handler ──────────────────────────────────────────────────

    private void handleFallback(AgentContext context, Exception error) {
        if (error != null) {
            log.warn("[ARCHITECT:{}] Falling back due to error: {}",
                    context.getTraceId(), error.getMessage());
        }

        context.setFinalReply("""
                I'm currently processing your request using my built-in knowledge base.

                Based on your question, here are some general recommendations:

                - **Architecture**: Start with a modular monolith, extract microservices as needed
                - **Database**: PostgreSQL for structured data, Redis for caching
                - **Auth**: JWT with refresh tokens for stateless authentication
                - **Scaling**: Horizontal scaling with load balancer and read replicas

                For more detailed, AI-powered analysis, please ensure the AI service is configured.

                Feel free to ask a more specific question about any of these areas!
                """);
        context.setSource("FALLBACK");
        context.addStep(AgentStep.builder()
                .name("Fallback Response")
                .order(99)
                .status("COMPLETED")
                .output("Used fallback due to: " + (error != null ? error.getMessage() : "LLM unavailable"))
                .durationMs(0)
                .build());
    }

    // ─── Utility ───────────────────────────────────────────────────────────

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
