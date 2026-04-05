package com.systemforge.backend.architect.decision;

import com.systemforge.backend.architect.dto.AgentStep;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Executes a sequence of {@link DecisionStep}s to process a design request.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DecisionPipeline {

    private final List<DecisionStep> steps;

    /**
     * Run all applicable steps in order.
     */
    public void execute(AgentContext context) {
        log.info("[ARCHITECT:{}] Starting decision pipeline with {} steps",
                context.getTraceId(), steps.size());

        List<DecisionStep> sortedSteps = steps.stream()
                .sorted(Comparator.comparingInt(DecisionStep::order))
                .toList();

        for (DecisionStep step : sortedSteps) {
            if (!step.shouldExecute(context)) continue;

            long startMs = System.currentTimeMillis();
            try {
                log.debug("[ARCHITECT:{}] Executing step: {}", context.getTraceId(), step.name());
                StepResult result = step.execute(context);
                long durationMs = System.currentTimeMillis() - startMs;

                // Save structured data to context for downstream steps
                if (result.getStructuredData() != null) {
                    context.putStepOutput(step.name(), result.getStructuredData());
                }

                // Add to audit trail
                context.addStep(AgentStep.builder()
                        .name(step.name())
                        .order(step.order())
                        .status(result.getStatus())
                        .output(result.getSummary())
                        .durationMs(durationMs)
                        .build());

                if ("FAILED".equals(result.getStatus())) {
                    log.warn("[ARCHITECT:{}] Step failed: {}", context.getTraceId(), step.name());
                    // Decide whether to abort or continue.
                    // For now, we continue with degraded state.
                }

            } catch (Exception e) {
                long durationMs = System.currentTimeMillis() - startMs;
                log.error("[ARCHITECT:{}] Step threw exception: {}", context.getTraceId(), step.name(), e);

                context.addStep(AgentStep.builder()
                        .name(step.name())
                        .order(step.order())
                        .status("FAILED")
                        .output("Internal error: " + e.getMessage())
                        .durationMs(durationMs)
                        .build());
            }
        }

        // After all steps, generate final response
        generateFinalResponse(context);
    }

    private void generateFinalResponse(AgentContext context) {
        StringBuilder finalMarkdown = new StringBuilder();

        for (AgentStep step : context.getSteps()) {
            if ("COMPLETED".equals(step.getStatus()) && step.getOutput() != null) {
                finalMarkdown.append("## ").append(step.getName()).append("\n");
                finalMarkdown.append(step.getOutput()).append("\n\n");
            }
        }

        context.setFinalReply(finalMarkdown.toString().trim());
    }
}
