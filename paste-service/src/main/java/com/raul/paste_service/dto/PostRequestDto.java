package com.raul.paste_service.dto;

public record PostRequestDto(
        String content,
        Integer userId,
        Integer days
) { }
