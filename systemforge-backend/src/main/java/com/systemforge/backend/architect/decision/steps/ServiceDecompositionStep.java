package com.systemforge.backend.architect.decision.steps;

import com.systemforge.backend.architect.decision.DecisionStep;
import com.systemforge.backend.architect.decision.StepResult;
import com.systemforge.backend.architect.dto.ParsedRequirements;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceDecompositionStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "Service Decomposition";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        ParsedRequirements reqs = context.getStepOutput("Requirement Analysis", ParsedRequirements.class);
        String architectureType = context.getStepOutput("Architecture Selection", String.class);
        
        if (reqs == null || architectureType == null) {
            return StepResult.builder().status("FAILED").summary("Missing pre-requisites").build();
        }

        String prompt = String.format(
                "Given the selected architecture: '%s', break down the %s system into domain services/modules. \n" +
                "Features required: %s\n\n" +
                "For each service, provide:\n" +
                "- Name\n" +
                "- Primary Responsibility\n" +
                "- Integration points with other services\n\n" +
                "Keep it practical. Don't over-complicate or create too many microservices unless specified.",
                architectureType, reqs.getAppType(), String.join(", ", reqs.getPrimaryFeatures())
        );

        LlmResponse response = llmClient.complete(
                "You are a domain-driven design expert. Decompose the system into cohesive services.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
