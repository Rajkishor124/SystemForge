package com.systemforge.backend.chat.controller;

import com.systemforge.backend.chat.dto.ChatRequest;
import com.systemforge.backend.chat.dto.ChatResponse;
import com.systemforge.backend.chat.service.ChatService;
import com.systemforge.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Architecture assistant powered by AI or rule engine")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(
            summary = "Send a chat message",
            description = "Send a message and receive architecture advice. Uses OpenAI when available, falls back to rule engine."
    )
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("Chat response generated", response));
    }
}
