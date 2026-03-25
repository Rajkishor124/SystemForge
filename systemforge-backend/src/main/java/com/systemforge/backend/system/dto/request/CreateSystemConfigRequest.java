package com.systemforge.backend.system.dto.request;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a new architecture design session.
 *
 * <p>Bean validation annotations enforce input correctness before
 * the request ever reaches the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new system architecture configuration")
public class CreateSystemConfigRequest {

    @NotBlank(message = "Config name must not be blank")
    @Size(min = 3, max = 150, message = "Config name must be between 3 and 150 characters")
    @Schema(description = "Human-readable name for this config", example = "My Ride App - V1")
    private String configName;

    @NotNull(message = "App type is required")
    @Schema(description = "Type of application being designed", example = "RIDE_HAILING")
    private AppType appType;

    @NotNull(message = "App scale is required")
    @Schema(description = "Target scale of the application", example = "MEDIUM")
    private AppScale appScale;

    @NotBlank(message = "Selected systems JSON must not be blank")
    @Schema(
            description = "JSON array of selected system IDs and their configurations",
            example = "[{\"systemId\": \"uuid\", \"customConfig\": {\"authType\": \"OTP\"}}]"
    )
    private String selectedSystemsJson;
}