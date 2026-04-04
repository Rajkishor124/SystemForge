package com.systemforge.backend.chat.repository;

import com.systemforge.backend.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
