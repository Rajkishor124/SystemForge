package com.systemforge.backend.architect.decision;

import com.systemforge.backend.architect.orchestrator.AgentContext;

/**
 * Represents a single reasoning step in the AI Architect's decision pipeline.
 */
public interface DecisionStep {

    /** Identifier for the step (e.g., "RequirementAnalysis"). */
    String name();

    /** Execution order in the pipeline. Lower numbers run first. */
    int order();

    /** Whether this step should execute in the current context. */
    boolean shouldExecute(AgentContext context);

    /** Execute the step, optionally using the LLM. */
    StepResult execute(AgentContext context);
}
