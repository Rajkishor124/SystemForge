package com.systemforge.backend.architect.repository;

import com.systemforge.backend.architect.entity.ArchitectStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchitectStepRepository extends JpaRepository<ArchitectStepEntity, UUID> {

    List<ArchitectStepEntity> findByMessageIdOrderByStepOrderAsc(UUID messageId);
}
