package com.example.user_service.dto;

public record UserResponseDto(
        String firstName,
        String lastName,
        String email
) {
}
