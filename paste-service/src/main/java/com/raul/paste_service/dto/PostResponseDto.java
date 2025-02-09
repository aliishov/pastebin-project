package com.raul.paste_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record PostResponseDto(
        String title,
        String content,
        Integer userId,
        Integer likesCount,
        Integer viewsCount,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expirationDate,
        String hash
) { }
