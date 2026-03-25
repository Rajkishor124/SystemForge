package com.systemforge.backend.template.service.impl;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.template.dto.TemplateDto;
import com.systemforge.backend.template.entity.Template;
import com.systemforge.backend.template.mapper.TemplateMapper;
import com.systemforge.backend.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private Template template;
    private TemplateDto templateDto;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        template = new Template();
        template.setId(id);
        template.setAppType(AppType.SAAS);
        template.setAppScale(AppScale.MEDIUM);
        template.setSystemType(SystemType.POSTGRESQL);

        templateDto = new TemplateDto();
        templateDto.setId(id);
        templateDto.setAppType(AppType.SAAS);
        templateDto.setSystemType(SystemType.POSTGRESQL);
    }

    @Test
    void getAllTemplates_returnsList() {
        when(templateRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        List<TemplateDto> result = templateService.getAllTemplates();

        assertEquals(1, result.size());
        assertEquals(templateDto, result.get(0));
    }

    @Test
    void getByAppType_returnsFilteredList() {
        when(templateRepository.findByAppTypeAndIsActiveTrueOrderBySortOrderAsc(AppType.SAAS))
                .thenReturn(List.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        List<TemplateDto> result = templateService.getByAppType(AppType.SAAS);

        assertEquals(1, result.size());
    }

    @Test
    void getByAppTypeAndScale_returnsFilteredList() {
        when(templateRepository.findByAppTypeAndAppScaleAndIsActiveTrueOrderBySortOrderAsc(AppType.SAAS, AppScale.MEDIUM))
                .thenReturn(List.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        List<TemplateDto> result = templateService.getByAppTypeAndScale(AppType.SAAS, AppScale.MEDIUM);

        assertEquals(1, result.size());
    }

    @Test
    void getById_found_returnsDto() {
        UUID id = template.getId();
        when(templateRepository.findById(id)).thenReturn(Optional.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        TemplateDto result = templateService.getById(id);

        assertEquals(templateDto, result);
    }

    @Test
    void getById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getById(id));
    }
}
