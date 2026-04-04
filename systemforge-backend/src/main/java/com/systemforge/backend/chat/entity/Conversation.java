package com.systemforge.backend.chat.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        indexes = {
                @Index(name = "idx_conv_user_id", columnList = "user_id"),
                @Index(name = "idx_conv_created_at", columnList = "created_at")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 255)
    @Builder.Default
    private String title = "New Conversation";
}
