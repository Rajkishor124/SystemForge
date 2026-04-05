package com.systemforge.backend.playground.engine;

import com.systemforge.backend.playground.template.TemplateStructure;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated placeholder resolution system.
 *
 * <p>Takes a {@link TemplateStructure} and a {@link TemplateCompositionContext},
 * replaces all {@code {{PLACEHOLDER}}} tokens in every template section,
 * and produces the final resolved code.
 *
 * <p>Design rules:
 * <ul>
 *   <li>Feature modules NEVER directly modify final code</li>
 *   <li>They ONLY modify placeholders in the context</li>
 *   <li>This resolver is the SINGLE point where tokens become code</li>
 * </ul>
 */
@Component
public class PlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Resolves all placeholders in the template structure using the context's placeholder map.
     *
     * @return a new TemplateStructure with all tokens replaced
     */
    public TemplateStructure resolve(TemplateStructure template, TemplateCompositionContext context) {
        Map<String, String> placeholders = context.getPlaceholders();

        return TemplateStructure.builder()
                .controllerTemplate(resolveSection(template.getControllerTemplate(), placeholders))
                .serviceTemplate(resolveSection(template.getServiceTemplate(), placeholders))
                .configTemplate(resolveSection(template.getConfigTemplate(), placeholders))
                .securityTemplate(resolveSection(template.getSecurityTemplate(), placeholders))
                .build();
    }

    /**
     * Resolves a single architecture description string.
     */
    public String resolveText(String text, TemplateCompositionContext context) {
        if (text == null) return "";
        return resolveSection(text, context.getPlaceholders());
    }

    private String resolveSection(String section, Map<String, String> placeholders) {
        if (section == null) return "";

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(section);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.getOrDefault(key, "");
            // Escape regex special chars in replacement
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
