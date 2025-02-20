package com.example.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ResendConfirmationRequest(
        @NotEmpty(message = "Email should not be empty")
        @NotBlank(message = "Email name should not be empty")
        @Email
        String email
) {
}
