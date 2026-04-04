package com.systemforge.backend.chat.controller;

import com.systemforge.backend.chat.dto.*;
import com.systemforge.backend.chat.service.ChatService;
import com.systemforge.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Architecture assistant powered by AI or rule engine")
public class ChatController {

    private final ChatService chatService;

    // ─── Legacy endpoint (backward compat) ─────────────────────────────────────

    @PostMapping
    @Operation(summary = "Send a chat message (legacy)", description = "Stateless chat — no persistence.")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("Chat response generated", response));
    }

    // ─── Conversation CRUD ─────────────────────────────────────────────────────

    @PostMapping("/conversations")
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation() {
        ConversationDto dto = chatService.createConversation();
        return ResponseEntity.ok(ApiResponse.success("Conversation created", dto));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List all conversations for the authenticated user")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> listConversations() {
        List<ConversationDto> list = chatService.listConversations();
        return ResponseEntity.ok(ApiResponse.success("Conversations loaded", list));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get a conversation with all messages")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> getConversation(
            @PathVariable UUID id
    ) {
        ConversationDetailDto detail = chatService.getConversation(id);
        return ResponseEntity.ok(ApiResponse.success("Conversation loaded", detail));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send a message in a conversation and get AI response")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        ChatMessageDto reply = chatService.sendMessage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent", reply));
    }

    @PutMapping("/conversations/{id}")
    @Operation(summary = "Rename a conversation")
    public ResponseEntity<ApiResponse<ConversationDto>> renameConversation(
            @PathVariable UUID id,
            @Valid @RequestBody RenameConversationRequest request
    ) {
        ConversationDto dto = chatService.renameConversation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Conversation renamed", dto));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id
    ) {
        chatService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted", null));
    }
}
