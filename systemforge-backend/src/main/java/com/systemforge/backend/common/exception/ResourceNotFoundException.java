package com.systemforge.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist or is soft-deleted.
 *
 * <p>Maps to HTTP 404 Not Found. Service layer should throw this rather than
 * returning null or Optional.empty(), keeping the controller layer clean.
 *
 * <p>Example usage:
 * <pre>{@code
 *   userRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("USR_001", "User not found with id: " + id));
 * }</pre>
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}