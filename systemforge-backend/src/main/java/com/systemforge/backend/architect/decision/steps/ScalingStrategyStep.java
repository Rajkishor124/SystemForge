package com.systemforge.backend.architect.decision.steps;

import com.systemforge.backend.architect.decision.DecisionStep;
import com.systemforge.backend.architect.decision.StepResult;
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
public class ScalingStrategyStep implements DecisionStep {

    private final LlmClient llmClient;

    @Override
    public String name() {
        return "Scaling Strategy";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Long> metrics = context.getStepOutput("Scale Estimation", Map.class);
        
        if (metrics == null) {
            return StepResult.builder().status("SKIPPED").summary("No scale metrics available").build();
        }

        Long ccu = metrics.getOrDefault("ccu", 5000L);
        Long qps = metrics.getOrDefault("qps", 100L);

        String prompt = String.format(
                "Provide a concrete infrastructure scaling strategy for a system handling:\n" +
                "- %d Concurrent Users\n" +
                "- %d QPS\n\n" +
                "What caching tiers, read replicas, or message queues are necessary? " +
                "Should this run on EC2/VMs, Kubernetes, or Serverless?",
                ccu, qps
        );

        LlmResponse response = llmClient.complete(
                "You are an SRE and Cloud Infrastructure expert.",
                prompt
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(response.getContent())
                .structuredData(response.getContent())
                .build();
    }
}
