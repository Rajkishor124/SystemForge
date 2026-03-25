package com.systemforge.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request to initiate OTP-based login. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to send OTP to an email address")
public class SendOtpRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    @Schema(example = "arjun@systemforge.io")
    private String email;
}