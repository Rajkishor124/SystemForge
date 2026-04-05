package com.systemforge.backend.architect.repository;

import com.systemforge.backend.architect.entity.ArchitectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchitectMessageRepository extends JpaRepository<ArchitectMessage, UUID> {

    List<ArchitectMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    long countBySessionId(UUID sessionId);
}
