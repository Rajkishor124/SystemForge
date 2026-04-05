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
public class ApiDesignStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "API Design";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        ParsedRequirements reqs = context.getStepOutput("Requirement Analysis", ParsedRequirements.class);
        if (reqs == null) {
            return StepResult.builder().status("FAILED").summary("Missing pre-requisites").build();
        }

        String services = context.getStepOutput("Service Decomposition", String.class);

        String prompt = String.format(
                "Based on the following services:\n%s\n\n" +
                "What API styles should be used (e.g., REST, GraphQL, gRPC, WebSockets)?\n" +
                "Detail the API Gateway routing and identify 3-5 critical endpoints.",
                services != null ? services : "Features: " + String.join(", ", reqs.getPrimaryFeatures())
        );

        LlmResponse response = llmClient.complete(
                "You are an API design expert. Select the right protocol for the right job.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
