package com.systemforge.backend.common.exception;

import com.systemforge.backend.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all SystemForge controllers.
 *
 * <p>Centralizes error handling so individual controllers stay thin.
 * Every exception that escapes a service method is caught here and mapped
 * to a consistent {@link ApiResponse} with the correct HTTP status.
 *
 * <p>Internal exception details (stack traces, DB errors) are NEVER exposed
 * to the client — they are logged server-side only.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── DOMAIN / BUSINESS EXCEPTIONS ────────────────────────────────────────

    /**
     * Handles all custom domain exceptions ({@link BusinessException} and subclasses).
     * The HTTP status and error code are pulled from the exception itself.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("[BusinessException] errorCode={} message={} path={}",
                ex.getErrorCode(), ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─── VALIDATION EXCEPTIONS ────────────────────────────────────────────────

    /**
     * Handles @Valid / @Validated failures on request DTOs.
     *
     * <p>Returns a map of field → error message so the client can highlight
     * specific form fields without parsing the message string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid value",
                        // Keep the first error if multiple violations exist on the same field
                        (existing, duplicate) -> existing
                ));

        log.warn("[ValidationException] fields={}", fieldErrors.keySet());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("Validation failed", fieldErrors));
    }

    // ─── SECURITY EXCEPTIONS ─────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("[AccessDenied] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied — insufficient permissions"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("[BadCredentials] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials"));
    }

    // ─── FALLBACK ─────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unexpected exception.
     *
     * <p>Logs the full stack trace server-side but returns a generic message to the client.
     * This ensures internal implementation details are never leaked.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception ex, WebRequest request) {

        log.error("[UnhandledException] path={} message={}",
                request.getDescription(false), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}