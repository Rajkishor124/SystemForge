package com.systemforge.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Root exception for all business-layer errors in SystemForge.
 *
 * <p>Any intentional domain violation (e.g., "user already exists", "invalid system config")
 * should throw a subclass of this — never a raw RuntimeException.
 *
 * <p>Why a custom hierarchy instead of Spring's exceptions?
 * <ul>
 *   <li>Domain errors carry an {@link HttpStatus} so the global handler maps them cleanly</li>
 *   <li>An {@code errorCode} (e.g., "AUTH_001") lets clients handle specific errors programmatically</li>
 *   <li>Decouples HTTP semantics from service layer — services throw business exceptions,
 *       the handler decides the HTTP response</li>
 * </ul>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Machine-readable error code for client-side programmatic handling.
     * Convention: MODULE_NNN, e.g., "AUTH_001", "SYS_003".
     */
    private final String errorCode;

    /**
     * HTTP status this exception maps to.
     * Defaulted to 400 BAD_REQUEST for generic business violations.
     */
    private final HttpStatus httpStatus;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}