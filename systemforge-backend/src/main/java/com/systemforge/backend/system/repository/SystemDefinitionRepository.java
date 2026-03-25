package com.systemforge.backend.system.repository;

import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.system.entity.SystemDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemDefinitionRepository extends JpaRepository<SystemDefinition, UUID> {

    List<SystemDefinition> findBySystemTypeAndIsActiveTrue(SystemType systemType);

    List<SystemDefinition> findByIsActiveTrue();
}