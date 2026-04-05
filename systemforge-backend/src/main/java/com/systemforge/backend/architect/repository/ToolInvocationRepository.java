package com.systemforge.backend.architect.repository;

import com.systemforge.backend.architect.entity.ToolInvocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToolInvocationRepository extends JpaRepository<ToolInvocationEntity, UUID> {

    List<ToolInvocationEntity> findByMessageIdOrderByCreatedAtAsc(UUID messageId);
}
