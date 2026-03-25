package com.systemforge.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Structured error detail payload.
 *
 * <p>Used in the {@code data} field of {@link ApiResponse} on error responses,
 * specifically for business exceptions that carry a machine-readable error code.
 *
 * <p>This allows clients (web, mobile) to react programmatically to specific errors
 * without parsing human-readable messages.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

    /** Machine-readable error code. Convention: MODULE_NNN, e.g., AUTH_001. */
    private final String errorCode;

    /** Field name for field-level errors (null for entity-level errors). */
    private final String field;

    /** Human-readable explanation of the error. */
    private final String detail;

    public static ErrorDetail of(String errorCode, String detail) {
        return ErrorDetail.builder()
                .errorCode(errorCode)
                .detail(detail)
                .build();
    }

    public static ErrorDetail ofField(String field, String detail) {
        return ErrorDetail.builder()
                .field(field)
                .detail(detail)
                .build();
    }
}