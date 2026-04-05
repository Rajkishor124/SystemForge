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
public class ArchitectureSelectionStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "Architecture Selection";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        ParsedRequirements reqs = context.getStepOutput("Requirement Analysis", ParsedRequirements.class);
        if (reqs == null) {
            return StepResult.builder().status("FAILED").summary("Missing requirements").build();
        }

        String prompt = String.format(
                "Select the best architectural pattern (e.g., Modular Monolith, Microservices, Event-Driven) " +
                "for a %s application at %s scale. \n\nFeatures: %s\nConstraints: %s\n\n" +
                "Respond with a short paragraph naming the selected pattern and your justification.",
                reqs.getAppType(), reqs.getAppScale(),
                String.join(", ", reqs.getPrimaryFeatures()),
                String.join(", ", reqs.getConstraints())
        );

        LlmResponse response = llmClient.complete(
                "You are an expert system architect. Be decisive and concise.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
