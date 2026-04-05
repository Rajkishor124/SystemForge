package com.systemforge.backend.architect.decision.steps;

import com.systemforge.backend.architect.decision.DecisionStep;
import com.systemforge.backend.architect.decision.StepResult;
import com.systemforge.backend.architect.dto.ParsedRequirements;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ScaleEstimationStep implements DecisionStep {

    @Override
    public String name() {
        return "Scale Estimation";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        return "SYSTEM_DESIGN".equals(context.getIntent());
    }

    @Override
    public StepResult execute(AgentContext context) {
        ParsedRequirements reqs = context.getStepOutput("Requirement Analysis", ParsedRequirements.class);
        if (reqs == null) {
            return StepResult.builder().status("SKIPPED").summary("No requirements found").build();
        }

        int ccu = reqs.getEstimatedConcurrentUsers();
        if (ccu <= 0) {
            ccu = switch (reqs.getAppScale()) {
                case "LARGE" -> 500_000;
                case "MEDIUM" -> 50_000;
                default -> 5_000;
            };
        }

        // Rule of thumb estimations
        long estimatedHitsPerSecond = ccu / 10;
        long estimatedDbWritesPerSecond = estimatedHitsPerSecond / 5;
        
        // Save estimates for downstream steps
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("ccu", (long) ccu);
        metrics.put("qps", estimatedHitsPerSecond);
        metrics.put("tps", estimatedDbWritesPerSecond);

        String summary = String.format(
                "Based on the scale requirements, the system must handle:\n" +
                "- **%d** peak concurrent users\n" +
                "- **~%d** queries per second (QPS)\n" +
                "- **~%d** database writes per second",
                ccu, estimatedHitsPerSecond, estimatedDbWritesPerSecond
        );

        return StepResult.builder()
                .status("COMPLETED")
                .summary(summary)
                .structuredData(metrics)
                .build();
    }
}
