package com.systemforge.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a create/update operation would violate a uniqueness constraint.
 *
 * <p>Maps to HTTP 409 Conflict — semantically more correct than 400 for duplicates.
 *
 * <p>Example:
 * <pre>{@code
 *   if (userRepository.existsByEmail(email)) {
 *       throw new DuplicateResourceException("USR_002", "Email already registered: " + email);
 *   }
 * }</pre>
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}