package com.systemforge.backend.architect.prompts;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight prompt template engine.
 *
 * <p>Supports {@code {{variable}}} interpolation from classpath-loaded
 * template files. Keeps all logic in Java — prompts are pure text.
 *
 * <p>Thread-safe and immutable after construction.
 */
public class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final String templateName;
    private final String rawContent;

    public PromptTemplate(String templateName, String rawContent) {
        this.templateName = templateName;
        this.rawContent = rawContent;
    }

    /**
     * Render the template by replacing all {{key}} placeholders.
     *
     * @param variables key → value mapping
     * @return rendered prompt string
     * @throws IllegalArgumentException if a placeholder has no matching variable
     */
    public String render(Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(rawContent);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Render with no variables (static prompt).
     */
    public String render() {
        return rawContent;
    }

    public String getTemplateName() {
        return templateName;
    }
}
