package com.systemforge.backend.system.dto;

import com.systemforge.backend.common.enums.SystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** External representation of a SystemDefinition catalog entry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemDefinitionDto {
    private UUID id;
    private String name;
    private SystemType systemType;
    private String description;
    private String configSchema;
    private boolean active;
}