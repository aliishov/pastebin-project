package com.example.user_service.dto;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String nickname,
        String email
) {
}
