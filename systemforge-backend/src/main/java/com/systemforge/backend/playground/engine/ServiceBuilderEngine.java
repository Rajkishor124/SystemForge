package com.systemforge.backend.playground.engine;

import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;

/**
 * Core contract for the playground generation engine.
 *
 * <p>Implementations must be:
 * <ul>
 *   <li>Stateless — no instance-level mutable state</li>
 *   <li>Polymorphic — no if-else for specific features/services</li>
 *   <li>Composable — all logic delegated to templates and feature modules</li>
 * </ul>
 */
public interface ServiceBuilderEngine {

    /**
     * Generates a complete playground output from a validated config request.
     *
     * <p>The request MUST be validated before calling this method.
     * The engine does NOT perform validation.
     */
    PlaygroundGeneratedOutput generate(PlaygroundConfigRequest request);
}
