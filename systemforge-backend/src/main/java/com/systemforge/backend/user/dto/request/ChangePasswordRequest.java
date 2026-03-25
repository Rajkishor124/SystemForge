package com.systemforge.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Request DTO for changing user password.
 *
 * Security Rules:
 * - Current password must be provided
 * - New password must meet strength requirements
 */
@Getter
@Schema(description = "Request payload for changing user password")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "User's current password")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Schema(description = "New password (must be strong)", example = "StrongPass@123")
    private String newPassword;

    /**
     * Optional helper for trimming
     */
    public String getCurrentPassword() {
        return currentPassword != null ? currentPassword.trim() : null;
    }

    public String getNewPassword() {
        return newPassword != null ? newPassword.trim() : null;
    }
}