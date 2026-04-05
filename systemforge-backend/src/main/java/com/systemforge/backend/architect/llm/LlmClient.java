package com.systemforge.backend.architect.llm;

/**
 * Abstraction over LLM providers (OpenAI, Claude, local models).
 *
 * <p>This interface is the ONLY contract between the AI Architect
 * agent and any language model. Implementations are swappable
 * without changing application logic.
 *
 * <p>Design decisions:
 * <ul>
 *     <li>Text completions for free-form reasoning</li>
 *     <li>Structured completions for typed JSON output</li>
 *     <li>System + user message separation for prompt injection safety</li>
 * </ul>
 */
public interface LlmClient {

    /**
     * Send a prompt and receive a free-form text response.
     *
     * @param systemPrompt the system-level instruction (persona, rules)
     * @param userPrompt   the user-facing message
     * @return structured response with content and metadata
     */
    LlmResponse complete(String systemPrompt, String userPrompt);

    /**
     * Send a prompt and receive a structured (JSON) response
     * matching the given type.
     *
     * @param systemPrompt the system-level instruction
     * @param userPrompt   the user-facing message
     * @param responseType the target class for deserialization
     * @return structured response with typed content
     */
    <T> T completeStructured(String systemPrompt, String userPrompt, Class<T> responseType);

    /**
     * Check if this LLM client is available and operational.
     */
    boolean isAvailable();
}
