package com.systemforge.backend.chat.service;

import com.systemforge.backend.chat.dto.*;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    /** Legacy endpoint - kept for backward compatibility */
    ChatResponse chat(ChatRequest request);

    /** Create a new conversation for the authenticated user */
    ConversationDto createConversation();

    /** List conversations for the authenticated user */
    List<ConversationDto> listConversations();

    /** Get full conversation with messages */
    ConversationDetailDto getConversation(UUID conversationId);

    /** Send a message in a conversation and get AI response */
    ChatMessageDto sendMessage(UUID conversationId, SendMessageRequest request);

    /** Rename a conversation */
    ConversationDto renameConversation(UUID conversationId, RenameConversationRequest request);

    /** Soft-delete a conversation */
    void deleteConversation(UUID conversationId);
}
