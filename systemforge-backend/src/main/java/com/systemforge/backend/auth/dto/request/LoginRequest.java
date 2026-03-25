package com.systemforge.backend.auth.dto.request;
 
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/** Request body for email + password login. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Email and password login credentials")
public class LoginRequest {
 
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    private String email;
 
    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}