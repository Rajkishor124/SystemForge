package com.systemforge.backend.playground.engine;

import com.systemforge.backend.playground.template.TemplateStructure;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dedicated placeholder resolution system.
 *
 * <p>Takes a {@link TemplateStructure} and a {@link TemplateCompositionContext},
 * replaces all {@code {{PLACEHOLDER}}} tokens in every template section,
 * cleans up formatting artifacts, and produces the final resolved code.
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
    /** Matches 3+ consecutive blank lines → collapses to exactly 1 blank line */
    private static final Pattern EXCESSIVE_BLANK_LINES = Pattern.compile("(?m)(\\s*\\n){3,}");
    /** Matches lines that are only whitespace */
    private static final Pattern TRAILING_WHITESPACE_LINES = Pattern.compile("(?m)[ \\t]+$");

    /**
     * Resolves all placeholders in the template structure using the context's placeholder map,
     * and prepends the dynamically accumulated import block.
     *
     * @return a new TemplateStructure with all tokens replaced and imports injected
     */
    public TemplateStructure resolve(TemplateStructure template, TemplateCompositionContext context) {
        Map<String, String> placeholders = context.getPlaceholders();

        return TemplateStructure.builder()
                .controllerTemplate(buildSection(
                        template.getControllerTemplate(), placeholders,
                        context.getImports(), template.getControllerImports()))
                .serviceTemplate(buildSection(
                        template.getServiceTemplate(), placeholders,
                        context.getImports(), template.getServiceImports()))
                .configTemplate(buildSection(
                        template.getConfigTemplate(), placeholders,
                        context.getImports(), template.getConfigImports()))
                .securityTemplate(buildSection(
                        template.getSecurityTemplate(), placeholders,
                        context.getImports(), template.getSecurityImports()))
                .build();
    }

    /**
     * Resolves a single architecture description string.
     */
    public String resolveText(String text, TemplateCompositionContext context) {
        if (text == null) return "";
        return cleanFormatting(resolveTokens(text, context.getPlaceholders()));
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private String buildSection(
            String sectionTemplate,
            Map<String, String> placeholders,
            Set<String> dynamicImports,
            Set<String> baseImports
    ) {
        if (sectionTemplate == null) return "";

        // 1. Resolve placeholders
        String resolved = resolveTokens(sectionTemplate, placeholders);

        // 2. Build import block (base + dynamic, sorted, deduplicated)
        String importBlock = buildImportBlock(baseImports, dynamicImports);

        // 3. Prepend imports if any exist
        if (!importBlock.isEmpty()) {
            resolved = importBlock + "\n\n" + resolved;
        }

        // 4. Clean formatting artifacts
        return cleanFormatting(resolved);
    }

    private String resolveTokens(String section, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(section);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String buildImportBlock(Set<String> baseImports, Set<String> dynamicImports) {
        Set<String> merged = new java.util.TreeSet<>(baseImports != null ? baseImports : Set.of());
        if (dynamicImports != null) {
            merged.addAll(dynamicImports);
        }

        if (merged.isEmpty()) return "";

        return merged.stream()
                .map(imp -> "import " + imp + ";")
                .collect(Collectors.joining("\n"));
    }

    /**
     * Cleans formatting artifacts left by placeholder replacement:
     * - Removes trailing whitespace from lines
     * - Collapses 3+ consecutive blank lines into 1
     */
    private String cleanFormatting(String code) {
        String cleaned = TRAILING_WHITESPACE_LINES.matcher(code).replaceAll("");
        cleaned = EXCESSIVE_BLANK_LINES.matcher(cleaned).replaceAll("\n\n");
        return cleaned.strip();
    }
}
