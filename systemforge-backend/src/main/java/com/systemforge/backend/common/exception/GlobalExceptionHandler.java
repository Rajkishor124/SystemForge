package com.systemforge.backend.common.exception;

import com.systemforge.backend.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Global exception handler for all SystemForge controllers.
 *
 * <p>Centralizes error handling so individual controllers stay thin.
 * Every exception that escapes a service method is caught here and mapped
 * to a consistent {@link ApiResponse} with the correct HTTP status and
 * machine-readable {@link ErrorCode}.
 *
 * <p>Internal exception details (stack traces, DB errors) are NEVER exposed
 * to the client — they are logged server-side only.
 */
@RestControllerAdvice
@Slf4j
@SuppressWarnings("null")
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
                .body(ApiResponse.error(
                        resolveErrorCode(ex.getErrorCode()),
                        ex.getMessage()));
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
                .body(ApiResponse.error(ErrorCode.VAL_001, "Validation failed", fieldErrors));
    }

    // ─── SECURITY EXCEPTIONS ─────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("[AccessDenied] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.GEN_003,
                        "Access denied — insufficient permissions"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("[BadCredentials] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.AUTH_001, "Invalid credentials"));
    }

    // ─── RESILIENCE EXCEPTIONS ───────────────────────────────────────────────

    /**
     * Circuit breaker is OPEN — all LLM calls are being rejected.
     * Returns 503 so clients know to retry later.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(
            CallNotPermittedException ex) {
        log.warn("[CircuitBreaker] Call rejected — circuit OPEN: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ErrorCode.AI_001,
                        "AI service temporarily unavailable. Please try again in a minute."));
    }

    /**
     * AI call timed out (beyond the configured threshold).
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeout(TimeoutException ex) {
        log.warn("[Timeout] AI call timed out: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error(ErrorCode.AI_002,
                        "AI request timed out. Please try again."));
    }

    // ─── SIZE LIMIT EXCEPTIONS ───────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {
        log.warn("[SizeLimit] Upload too large: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ErrorCode.VAL_003,
                        "Request body too large. Maximum allowed size is 10MB."));
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
                .body(ApiResponse.error(ErrorCode.GEN_004,
                        "An unexpected error occurred. Please try again later."));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Maps a BusinessException's string errorCode to an ErrorCode enum.
     * Falls back to GEN_004 if the code doesn't match any known enum value.
     */
    private ErrorCode resolveErrorCode(String code) {
        if (code == null || code.isBlank()) return ErrorCode.GEN_004;
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return ErrorCode.GEN_004;
        }
    }
}