package com.systemforge.backend.architect.decision.steps;

import com.systemforge.backend.architect.decision.DecisionStep;
import com.systemforge.backend.architect.decision.StepResult;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradeoffAnalysisStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "Tradeoff Analysis";
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        String dbOutput = context.getStepOutput("Database Design", String.class);
        String archOutput = context.getStepOutput("Architecture Selection", String.class);

        if (archOutput == null) {
            return StepResult.builder().status("FAILED").summary("Missing pre-requisites").build();
        }

        String prompt = String.format(
                "Review the selected architecture context:\n" +
                "Architecture: %s\n" +
                "Database choice: %s\n\n" +
                "Identify 2-3 major technical tradeoffs or risks in this design. " +
                "What alternatives were sacrificed? What are the potential pain points in production? " +
                "What actionable mitigations do you recommend?",
                archOutput, dbOutput != null ? dbOutput : "Not determined"
        );

        LlmResponse response = llmClient.complete(
                "You are a Staff Engineer. Be brutally honest about risks and tradeoffs.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
