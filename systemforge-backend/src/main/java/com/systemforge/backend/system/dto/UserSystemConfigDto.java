package com.systemforge.backend.system.dto;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** External representation of a user's architecture design session. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSystemConfigDto {
    private UUID id;
    private UUID userId;
    private String configName;
    private AppType appType;
    private AppScale appScale;
    private String selectedSystemsJson;
    private String generatedOutputJson;
    private boolean generated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}