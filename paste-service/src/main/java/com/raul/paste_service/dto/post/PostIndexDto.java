package com.raul.paste_service.dto.post;

import java.time.LocalDateTime;
import java.util.List;

public record PostIndexDto(
        Integer id,
        String title,
        String slug,
        String content,
        String summary,
        List<String> tags,
        String hash,
        Integer userId,
        Integer rating,
        Integer likesCount,
        Integer viewsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt,
        Boolean isDeleted
) {
}
