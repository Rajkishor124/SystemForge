package com.systemforge.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Message must not be blank")
    @Size(max = 4000, message = "Message must be at most 4000 characters")
    private String message;

    /**
     * Optional: Previous messages for context (simple conversation history).
     */
    private java.util.List<ChatMessage> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessage {
        private String role; // "user" or "assistant"
        private String content;
    }
}
