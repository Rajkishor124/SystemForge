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
}
