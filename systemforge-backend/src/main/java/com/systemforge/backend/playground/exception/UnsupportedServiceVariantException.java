package com.systemforge.backend.playground.exception;

import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a variant does not belong to the specified service type.
 * Maps to HTTP 400.
 */
public class UnsupportedServiceVariantException extends BusinessException {

    public UnsupportedServiceVariantException(ServiceType type, ServiceVariant variant) {
        super(
                "PLAYGROUND_001",
                String.format("Variant '%s' is not supported for service type '%s'", variant, type),
                HttpStatus.BAD_REQUEST
        );
    }
}
