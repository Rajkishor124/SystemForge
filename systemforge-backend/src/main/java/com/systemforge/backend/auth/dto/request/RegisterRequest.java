package com.systemforge.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for new user registration (Email/Password flow).
 *
 * <p>Password policy enforced at the DTO layer:
 * must be 8–72 chars, contain at least one digit and one letter.
 * BCrypt's practical limit is 72 bytes — anything longer is silently truncated,
 * so we cap input at 72 chars to avoid false security assumptions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "New user registration payload")
public class RegisterRequest {

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(example = "Arjun Sharma")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    @Schema(example = "arjun@systemforge.io")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one digit"
    )
    @Schema(example = "Secure123", description = "8–72 chars, must contain letter and digit")
    private String password;
}