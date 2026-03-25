package com.systemforge.backend.template.service;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.template.dto.TemplateDto;

import java.util.List;
import java.util.UUID;

public interface TemplateService {
    List<TemplateDto> getAllTemplates();
    List<TemplateDto> getByAppType(AppType appType);
    List<TemplateDto> getByAppTypeAndScale(AppType appType, AppScale appScale);
    TemplateDto getById(UUID templateId);
}