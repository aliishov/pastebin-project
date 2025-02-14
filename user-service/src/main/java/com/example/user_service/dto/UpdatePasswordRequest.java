package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(

        @NotBlank(message = "Current password should not be empty")
        String currentPassword,

        @NotBlank(message = "New password should not be empty")
        @Size(min = 8, message = "New password should be greater than 8 characters")
        String newPassword
) {
}
