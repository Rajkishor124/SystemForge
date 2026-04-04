package com.systemforge.backend.template.dto;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDto {
    private UUID id;
    private String name;
    private String description;
    private AppType appType;
    private SystemType systemType;
    private AppScale appScale;
    private String defaultPrompt;
    private String configJson;
    private boolean active;
    private int sortOrder;
}