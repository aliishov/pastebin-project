package com.raul.paste_service.dto.post;

import jakarta.validation.constraints.*;

import java.util.List;

public record PostRequestDto(

        @NotBlank(message = "Title should not be empty")
        @Size(max = 50, message = "Title should be smaller than 50 characters")
        String title,

        String slug,

        @NotBlank(message = "Content should not be empty")
        @Size(max = 5000, message = "Content should be smaller than 5000 characters")
        String content,

        @NotBlank(message = "Summary should not be empty")
        @Size(max = 255, message = "Summary should be smaller than 255 characters")
        String summary,

        List<String> tags,

        @PositiveOrZero(message = "UserId should be greater than or equal to 0")
        Integer days
) { }
