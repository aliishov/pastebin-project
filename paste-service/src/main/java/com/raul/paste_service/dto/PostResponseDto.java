package com.raul.paste_service.dto;

import java.time.LocalDateTime;

public record PostResponseDto(
        String content,
        Integer userId,
        LocalDateTime expirationDate,
        String hash
) { }
