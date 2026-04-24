package com.systemforge.backend.system.repository;

import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.system.entity.GenerationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {

    Optional<GenerationJob> findByIdAndUserId(UUID id, UUID userId);

    Page<GenerationJob> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByStatus(JobStatus status);

    java.util.List<GenerationJob> findByStatusAndStartedAtBefore(JobStatus status, java.time.LocalDateTime startedAt);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE GenerationJob j SET j.status = :newStatus, j.startedAt = :startedAt WHERE j.id = :jobId AND j.status = :expectedStatus")
    int updateStatusConditionally(UUID jobId, JobStatus newStatus, JobStatus expectedStatus, java.time.LocalDateTime startedAt);

    Optional<GenerationJob> findFirstByConfigIdAndStatusInOrderByCreatedAtDesc(UUID configId, java.util.List<JobStatus> statuses);
}
