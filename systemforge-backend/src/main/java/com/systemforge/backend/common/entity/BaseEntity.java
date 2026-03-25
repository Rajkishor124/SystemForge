package com.systemforge.backend.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity inherited by every domain entity in SystemForge.
 *
 * <p>Enforces:
 * <ul>
 *   <li>UUID primary key — database-agnostic, safe for future sharding/microservices</li>
 *   <li>Soft delete — no hard deletes in production; allows audit trail and recovery</li>
 *   <li>Full audit trail — who created/modified and when</li>
 *   <li>Optimistic locking — prevents lost updates under concurrent modification</li>
 * </ul>
 *
 * <p>Using {@code @MappedSuperclass} so this class is NOT mapped to its own table.
 * Each subclass gets these columns merged into its own table.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /**
     * UUID primary key.
     *
     * <p>Strategy: AUTO-generated at persistence time using UUID v4.
     * Avoids sequential integer IDs which can leak record counts and
     * cause hotspot issues in distributed environments.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Timestamp of record creation.
     * Automatically set by Spring Data JPA Auditing — never set manually.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of last modification.
     * Automatically updated on every merge — never set manually.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Principal (userId or system identifier) who created this record.
     * Populated by {@link com.systemforge.backend.common.config.AuditingConfig}.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Principal who last modified this record.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Soft delete flag.
     *
     * <p>No domain entity should ever be hard-deleted. Service layer must always
     * check {@code isDeleted = false} in queries. Use {@code @Where} annotation
     * on concrete entities for automatic filtering.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * Optimistic locking version counter.
     *
     * <p>Prevents lost update anomalies under concurrent modification.
     * JPA automatically increments this on each UPDATE and throws
     * {@link jakarta.persistence.OptimisticLockException} on version mismatch.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}