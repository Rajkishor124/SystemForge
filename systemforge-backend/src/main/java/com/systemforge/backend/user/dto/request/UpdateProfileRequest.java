package com.systemforge.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Request DTO for updating user profile.
 *
 * Only safe fields are allowed to be updated.
 * Sensitive fields like email, role, and authProvider are NOT allowed here.
 */
@Getter
@Schema(description = "Request payload for updating user profile")
public class UpdateProfileRequest {

    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "Arjun Sharma")
    private String name;

    /**
     * Optional helper method (not required, but useful)
     * Ensures trimmed input before usage.
     */
    public String getName() {
        return name != null ? name.trim() : null;
    }
}