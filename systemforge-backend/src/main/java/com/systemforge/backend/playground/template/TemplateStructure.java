package com.systemforge.backend.playground.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Section-based template structure for code generation.
 *
 * <p>Instead of a single monolithic code string, templates are split into
 * logical sections. Each section can contain {@code {{PLACEHOLDER}}} tokens
 * that feature modules populate via
 * {@link com.systemforge.backend.playground.engine.TemplateCompositionContext}.
 *
 * <p>This design ensures:
 * <ul>
 *   <li>Templates only define base structure — no feature logic</li>
 *   <li>Features inject code via placeholders, not string manipulation</li>
 *   <li>The frontend can display sections independently</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateStructure {

    /** REST controller code template */
    private String controllerTemplate;

    /** Service layer code template */
    private String serviceTemplate;

    /** Configuration class code template */
    private String configTemplate;

    /** Security configuration code template */
    private String securityTemplate;
}
