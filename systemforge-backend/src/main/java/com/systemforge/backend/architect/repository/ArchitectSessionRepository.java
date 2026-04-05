package com.systemforge.backend.architect.repository;

import com.systemforge.backend.architect.entity.ArchitectSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchitectSessionRepository extends JpaRepository<ArchitectSession, UUID> {

    List<ArchitectSession> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
