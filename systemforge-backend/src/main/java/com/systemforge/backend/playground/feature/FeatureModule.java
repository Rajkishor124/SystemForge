package com.systemforge.backend.playground.feature;

import com.systemforge.backend.playground.engine.TemplateCompositionContext;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;

import java.util.List;

/**
 * Plugin contract for feature modules.
 *
 * <p>Each feature module is a self-contained plugin that:
 * <ul>
 *   <li>Declares which {@link FeatureToggle} it handles</li>
 *   <li>Declares compatibility with service types and variants</li>
 *   <li>Declares dependencies on other feature toggles (for topological ordering)</li>
 *   <li>Applies itself to a {@link TemplateCompositionContext} by setting placeholders</li>
 * </ul>
 *
 * <p>Design rules:
 * <ul>
 *   <li>Modules modify ONLY placeholders and additive lists — never raw code</li>
 *   <li>Modules are stateless Spring beans</li>
 *   <li>Adding a new feature = implement this interface + register as @Component</li>
 * </ul>
 */
public interface FeatureModule {

    /**
     * Which toggle this module handles.
     */
    FeatureToggle getSupportedToggle();

    /**
     * Checks if this module can be applied to the given service type + variant.
     */
    boolean isCompatibleWith(ServiceType type, ServiceVariant variant);

    /**
     * Declares dependencies on other feature toggles.
     * The engine guarantees these are applied before this module.
     *
     * @return list of toggles that must be applied first (empty = no dependencies)
     */
    List<FeatureToggle> getDependencies();

    /**
     * Applies this feature to the composition context.
     * Modules MUST use context.setPlaceholder() / context.appendToPlaceholder()
     * and context.addComponent() / addArchitectureStep() / addTechStack().
     *
     * <p>Modules MUST NOT directly manipulate code strings.
     */
    void apply(TemplateCompositionContext context);
}
