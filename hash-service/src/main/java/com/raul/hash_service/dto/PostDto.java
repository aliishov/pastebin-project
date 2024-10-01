package com.raul.hash_service.dto;

import java.time.LocalDateTime;

public record PostDto(
        Integer id,
        String content,
        Integer userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt,
        Boolean isDeleted
) {
}
