package com.systemforge.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request to verify an OTP and complete authentication. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to verify OTP and obtain tokens")
public class VerifyOtpRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "OTP must not be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    @Schema(example = "482910")
    private String otp;
}