package com.example.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRestoreDto(
        @NotBlank(message = "Title should not be empty")
        @Email
        String email,

        @NotBlank(message = "Title should not be empty")
        String password
) {
}
