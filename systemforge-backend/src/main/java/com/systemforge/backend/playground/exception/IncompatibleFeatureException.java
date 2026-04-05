package com.systemforge.backend.playground.exception;

import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a feature toggle is not compatible with the selected service type,
 * or when a feature dependency is missing.
 * Maps to HTTP 422.
 */
public class IncompatibleFeatureException extends BusinessException {

    public IncompatibleFeatureException(FeatureToggle feature, ServiceType type) {
        super(
                "PLAYGROUND_002",
                String.format("Feature '%s' is not compatible with service type '%s'", feature, type),
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    public IncompatibleFeatureException(FeatureToggle feature, FeatureToggle missingDep) {
        super(
                "PLAYGROUND_003",
                String.format("Feature '%s' requires '%s' to be enabled first", feature, missingDep),
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
