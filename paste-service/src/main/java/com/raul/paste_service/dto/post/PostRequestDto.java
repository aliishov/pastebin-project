package com.raul.paste_service.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
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

        @FutureOrPresent(message = "Expiration date must be today or in the future")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
        LocalDateTime expirationDate
) { }
