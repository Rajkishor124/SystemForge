package com.systemforge.backend.architect.decision.steps;

import com.systemforge.backend.architect.decision.DecisionStep;
import com.systemforge.backend.architect.decision.StepResult;
import com.systemforge.backend.architect.dto.ParsedRequirements;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import com.systemforge.backend.architect.prompts.PromptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequirementAnalysisStep implements DecisionStep {

    private final LlmClient llmClient;
    private final PromptRegistry promptRegistry;

    @Override
    public String name() {
        return "Requirement Analysis";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        String prompt = promptRegistry.get("requirement_analysis").render(Map.of(
                "message", context.getUserMessage(),
                "context", context.getConversationContextString()
        ));

        ParsedRequirements reqs = llmClient.completeStructured(
                "You are an expert system analyst. Extract structured requirements.",
                prompt,
                ParsedRequirements.class
        );

        String summary = String.format(
                "Detected **%s** application at **%s** scale (%d concurrent users).\n\n" +
                "**Key Features:** %s\n" +
                "**Constraints:** %s",
                reqs.getAppType(),
                reqs.getAppScale(),
                reqs.getEstimatedConcurrentUsers(),
                String.join(", ", reqs.getPrimaryFeatures()),
                String.join(", ", reqs.getConstraints())
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(summary)
                .structuredData(reqs)
                .build();
    }
}
