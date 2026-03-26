package com.systemforge.backend.template.service.impl;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.template.dto.TemplateDto;
import com.systemforge.backend.template.mapper.TemplateMapper;
import com.systemforge.backend.template.repository.TemplateRepository;
import com.systemforge.backend.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "templates")
    public List<TemplateDto> getAllTemplates() {
        return templateRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(templateMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> getByAppType(AppType appType) {
        return templateRepository.findByAppTypeAndIsActiveTrueOrderBySortOrderAsc(appType)
                .stream().map(templateMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> getByAppTypeAndScale(AppType appType, AppScale appScale) {
        return templateRepository.findByAppTypeAndAppScaleAndIsActiveTrueOrderBySortOrderAsc(appType, appScale)
                .stream().map(templateMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDto getById(UUID templateId) {
        return templateRepository.findById(templateId)
                .map(templateMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TPL_001", "Template not found with id: " + templateId));
    }
}