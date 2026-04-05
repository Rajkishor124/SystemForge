package com.systemforge.backend.playground.engine;

import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured composition context passed through the feature module pipeline.
 *
 * <p>This is the ONLY way feature modules interact with the generation output.
 * Modules modify placeholders and add to lists — they NEVER manipulate raw code strings.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Mutable — modules mutate this context during the pipeline</li>
 *   <li>Placeholder-driven — all code injection happens via placeholder overrides</li>
 *   <li>Additive — components, stack, and architecture steps are appended</li>
 * </ul>
 */
@Data
public class TemplateCompositionContext {

    // ─── Identity ──────────────────────────────────────────────────────────────

    private final ServiceType serviceType;
    private final ServiceVariant variant;

    // ─── Placeholder map: feature modules override values here ─────────────────

    private final Map<String, String> placeholders;

    // ─── Additive lists: features append to these ──────────────────────────────

    private final List<String> components;
    private final List<String> architectureSteps;
    private final List<String> techStack;

    /**
     * Creates a context with defaults from the resolved template.
     */
    public TemplateCompositionContext(
            ServiceType serviceType,
            ServiceVariant variant,
            Map<String, String> defaultPlaceholders,
            List<String> defaultComponents,
            List<String> defaultStack
    ) {
        this.serviceType = serviceType;
        this.variant = variant;
        this.placeholders = new LinkedHashMap<>(defaultPlaceholders);
        this.components = new ArrayList<>(defaultComponents);
        this.architectureSteps = new ArrayList<>();
        this.techStack = new ArrayList<>(defaultStack);
    }

    // ─── Mutation API for feature modules ──────────────────────────────────────

    /**
     * Sets a placeholder value. Feature modules call this to inject code.
     */
    public void setPlaceholder(String key, String value) {
        placeholders.put(key, value);
    }

    /**
     * Appends content to an existing placeholder (useful when multiple features
     * contribute to the same section).
     */
    public void appendToPlaceholder(String key, String value) {
        placeholders.merge(key, value, (existing, addition) -> existing + "\n" + addition);
    }

    /**
     * Adds a component to the architecture breakdown.
     */
    public void addComponent(String component) {
        if (!components.contains(component)) {
            components.add(component);
        }
    }

    /**
     * Adds an architecture flow step.
     */
    public void addArchitectureStep(String step) {
        architectureSteps.add(step);
    }

    /**
     * Adds a tech stack entry.
     */
    public void addTechStack(String tech) {
        if (!techStack.contains(tech)) {
            techStack.add(tech);
        }
    }
}
