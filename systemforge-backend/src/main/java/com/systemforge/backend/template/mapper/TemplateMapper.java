package com.systemforge.backend.template.mapper;

import com.systemforge.backend.template.dto.TemplateDto;
import com.systemforge.backend.template.entity.Template;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TemplateMapper {
    TemplateDto toDto(Template template);
}