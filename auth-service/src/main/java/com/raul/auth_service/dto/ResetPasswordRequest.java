package com.raul.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotEmpty
        @NotBlank
        String token,

        @NotEmpty(message = "Password name should not be empty")
        @NotBlank(message = "Password name should not be empty")
        @Size(min = 8, message = "Password should be greater than 8 characters")
        String newPassword
) {
}
