package com.systemforge.backend.template.repository;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findByIsActiveTrueOrderBySortOrderAsc();

    List<Template> findByAppTypeAndIsActiveTrueOrderBySortOrderAsc(AppType appType);

    List<Template> findByAppTypeAndAppScaleAndIsActiveTrueOrderBySortOrderAsc(
            AppType appType, AppScale appScale);

    List<Template> findBySystemTypeAndIsActiveTrueOrderBySortOrderAsc(SystemType systemType);
}