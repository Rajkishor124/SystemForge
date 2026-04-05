package com.systemforge.backend.playground.template;

import java.util.List;
import java.util.Map;

/**
 * Contract for all service templates in the Playground.
 *
 * <p>Each template represents a specific ServiceType + ServiceVariant combination
 * (e.g., AUTH_JWT, PAYMENT_STRIPE). Templates are registered as Spring beans and
 * auto-discovered by {@link ServiceTemplateRegistry}.
 *
 * <p>Design rules:
 * <ul>
 *   <li>Templates define ONLY the base structure</li>
 *   <li>All dynamic parts use {@code {{PLACEHOLDER}}} tokens</li>
 *   <li>NO feature-specific logic inside templates</li>
 *   <li>Features inject into placeholders via the composition pipeline</li>
 * </ul>
 */
public interface ServiceTemplate {

    /**
     * Unique key identifying this template.
     * Convention: {@code SERVICETYPE_VARIANT} (e.g., {@code AUTH_JWT}).
     */
    String getKey();

    /**
     * Returns the section-based code template structure.
     */
    TemplateStructure getTemplateStructure();

    /**
     * Returns the base architecture description (before feature enrichment).
     */
    String getArchitectureDescription();

    /**
     * Returns the default component modules for this service variant.
     */
    List<String> getDefaultComponents();

    /**
     * Returns the recommended tech stack.
     */
    List<String> getRecommendedStack();

    /**
     * Returns default placeholder values that act as "empty" defaults.
     * Features will override these during the composition pipeline.
     *
     * <p>Example: {@code {{REFRESH_TOKEN_SECTION}} → "" } (empty by default,
     * populated by RefreshTokenModule if enabled).
     */
    Map<String, String> getDefaultPlaceholders();
}
