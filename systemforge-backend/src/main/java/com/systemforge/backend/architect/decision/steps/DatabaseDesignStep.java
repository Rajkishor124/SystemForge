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

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseDesignStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "Database Design";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        ParsedRequirements reqs = context.getStepOutput("Requirement Analysis", ParsedRequirements.class);
        String services = context.getStepOutput("Service Decomposition", String.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Long> metrics = context.getStepOutput("Scale Estimation", Map.class);
        
        if (reqs == null || services == null) {
            return StepResult.builder().status("FAILED").summary("Missing pre-requisites").build();
        }

        Long qps = metrics != null ? metrics.getOrDefault("qps", 100L) : 100L;

        String prompt = String.format(
                "Design the data layer for these services:\n%s\n\n" +
                "Estimated Load: ~%d QPS\n\n" +
                "Recommend the primary databases (SQL vs NoSQL vs In-Memory) and justify your choices based on CAP theorem and scale requirements.",
                services, qps
        );

        LlmResponse response = llmClient.complete(
                "You are an expert Data Architect. Focus on data models, constraints, and scalability.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
