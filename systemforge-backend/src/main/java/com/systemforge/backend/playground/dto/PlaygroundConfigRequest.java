package com.systemforge.backend.playground.dto;

import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Input DTO for generating a playground architecture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaygroundConfigRequest {

    @NotNull(message = "serviceType is required")
    private ServiceType serviceType;

    @NotNull(message = "variant is required")
    private ServiceVariant variant;

    @Builder.Default
    private List<FeatureToggle> features = new ArrayList<>();
}
