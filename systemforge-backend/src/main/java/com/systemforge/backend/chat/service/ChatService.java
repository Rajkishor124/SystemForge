package com.systemforge.backend.chat.service;

import com.systemforge.backend.chat.dto.ChatRequest;
import com.systemforge.backend.chat.dto.ChatResponse;

public interface ChatService {
    ChatResponse chat(ChatRequest request);
}
