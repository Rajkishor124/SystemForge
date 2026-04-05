package com.systemforge.backend.playground.repository;

import com.systemforge.backend.playground.entity.PlaygroundConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaygroundConfigRepository extends JpaRepository<PlaygroundConfig, UUID> {
    List<PlaygroundConfig> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
