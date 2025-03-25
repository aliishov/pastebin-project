package com.raul.paste_service.dto.review;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ReviewResponseDto(
        Integer postId,
        Integer userId,
        Integer grade,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
