package com.systemforge.backend.system.mapper;

import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.entity.SystemDefinition;
import com.systemforge.backend.system.entity.UserSystemConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemMapper {

    @Mapping(source = "active", target = "active")
    SystemDefinitionDto toDto(SystemDefinition entity);

    @Mapping(source = "generated", target = "generated")
    UserSystemConfigDto toDto(UserSystemConfig entity);
}