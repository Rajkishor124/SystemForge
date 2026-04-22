package com.systemforge.backend.common.security;

import java.util.regex.Pattern;

/**
 * Input sanitization utility for user-submitted text.
 *
 * <p>Strips HTML tags and dangerous characters to prevent:
 * <ul>
 *   <li>Stored XSS — malicious scripts persisted in the database</li>
 *   <li>Log injection — newlines/carriage returns in log-destined fields</li>
 *   <li>HTML injection — unstyled content rendered in emails/UIs</li>
 * </ul>
 *
 * <p>Usage: call in service layer before persisting user input.
 * Do NOT call on output — sanitize on input, encode on output.
 *
 * <pre>{@code
 *   config.setConfigName(InputSanitizer.sanitize(request.getConfigName()));
 * }</pre>
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Utility class — no instantiation
    }

    /** Matches any HTML tag (opening, closing, or self-closing). */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    /** Matches common script injection patterns. */
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)(javascript:|data:|vbscript:|on\\w+\\s*=)", Pattern.CASE_INSENSITIVE);

    /** Matches control characters (except newline/tab for multiline). */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");

    /**
     * Sanitizes a single-line text input.
     * Strips HTML, script injections, control chars, and normalizes whitespace.
     *
     * @param input raw user input
     * @return sanitized string, or null if input was null
     */
    public static String sanitize(String input) {
        if (input == null) return null;

        String cleaned = input;
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("[\\r\\n]+", " "); // Collapse newlines for single-line
        cleaned = cleaned.replaceAll("\\s{2,}", " "); // Normalize whitespace
        return cleaned.trim();
    }

    /**
     * Sanitizes multi-line text input (e.g., descriptions, chat messages).
     * Preserves newlines but strips HTML and script injections.
     *
     * @param input raw user input
     * @return sanitized string preserving line breaks, or null if input was null
     */
    public static String sanitizeMultiline(String input) {
        if (input == null) return null;

        String cleaned = input;
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    /**
     * Checks if a string contains potential XSS payloads.
     * Useful for validation without modification.
     *
     * @param input text to check
     * @return true if the input contains suspicious patterns
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isBlank()) return false;
        return HTML_TAG_PATTERN.matcher(input).find() ||
               SCRIPT_PATTERN.matcher(input).find();
    }
}
