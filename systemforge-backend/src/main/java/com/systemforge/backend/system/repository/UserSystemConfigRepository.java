package com.systemforge.backend.system.repository;

import com.systemforge.backend.system.entity.UserSystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSystemConfigRepository extends JpaRepository<UserSystemConfig, UUID> {

    Page<UserSystemConfig> findByUserId(UUID userId, Pageable pageable);

    Optional<UserSystemConfig> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByIsGeneratedTrue();
}