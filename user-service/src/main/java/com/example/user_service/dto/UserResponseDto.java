package com.example.user_service.dto;

public record UserResponseDto(
        String firstName,
        String lastName,
        String nickname,
        String email,
        String imageUrl
) {
}
