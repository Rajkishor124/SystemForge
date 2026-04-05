package com.systemforge.backend.playground.exception;

import com.systemforge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when no template is registered for the requested service type + variant.
 * Maps to HTTP 500 (this is a system configuration error, not a user error).
 */
public class TemplateNotFoundException extends BusinessException {

    public TemplateNotFoundException(String templateKey) {
        super(
                "PLAYGROUND_004",
                String.format("No template registered for key '%s'. Contact support.", templateKey),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
