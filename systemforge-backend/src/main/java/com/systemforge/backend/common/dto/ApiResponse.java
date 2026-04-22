package com.systemforge.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.systemforge.backend.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API response envelope for all SystemForge endpoints.
 *
 * <p>Every controller response — success or error — must be wrapped in this class.
 * This ensures a consistent contract between backend and all clients (web, mobile, API consumers).
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code @JsonInclude(NON_NULL)} — omits null fields from serialization, keeping payloads clean</li>
 *   <li>{@code correlationId} — enables distributed tracing across services and log correlation</li>
 *   <li>{@code timestamp} — ISO-8601 Instant for timezone-safe client parsing</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 *   return ResponseEntity.ok(ApiResponse.success("User created", userDto));
 *   return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
 * }</pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the operation was successful", example = "true")
    private final boolean success;

    @Schema(description = "Human-readable message describing the result", example = "User created successfully")
    private final String message;

    @Schema(description = "Response payload — null on error responses")
    private final T data;

    @Schema(description = "ISO-8601 UTC timestamp of the response")
    private final Instant timestamp;

    @Schema(description = "Correlation ID for distributed tracing — echo from X-Correlation-ID header")
    private final String correlationId;

    @Schema(description = "Machine-readable error code for programmatic handling (e.g., AUTH_001)")
    private final String errorCode;

    // ─── Factory methods — prefer these over the builder for standard cases ────

    /**
     * Creates a success response with data payload.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Creates a success response without a data payload (e.g., delete operations).
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Creates an error response with a structured error payload.
     * The {@code data} field carries error details (field violations, codes, etc.).
     */
    public static <T> ApiResponse<T> error(String message, T errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(errorDetails)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Creates a bare error response with no error detail payload.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Creates an error response with a machine-readable error code.
     */
    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(code.code())
                .message(message)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Creates an error response with a machine-readable error code and detail payload.
     */
    public static <T> ApiResponse<T> error(ErrorCode code, String message, T errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(code.code())
                .message(message)
                .data(errorDetails)
                .timestamp(Instant.now())
                .correlationId(CorrelationId.current())
                .build();
    }

    /**
     * Inner utility to retrieve the correlation ID from MDC without creating a hard dependency.
     */
    private static class CorrelationId {
        private static final String MDC_KEY = "correlationId";

        static String current() {
            return org.slf4j.MDC.get(MDC_KEY);
        }
    }
}