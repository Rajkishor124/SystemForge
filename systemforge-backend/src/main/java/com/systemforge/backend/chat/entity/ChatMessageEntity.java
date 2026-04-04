package com.systemforge.backend.chat.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chatmsg_conv_id", columnList = "conversation_id"),
                @Index(name = "idx_chatmsg_created_at", columnList = "created_at")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // "user" or "assistant"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 20)
    private String source; // "AI" or "RULE_ENGINE"
}
