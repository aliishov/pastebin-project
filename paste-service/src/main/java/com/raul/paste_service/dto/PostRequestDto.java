package com.raul.paste_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostRequestDto(

        @NotEmpty(message = "title name should not be empty")
        @NotBlank(message = "title name should not be empty")
        @Size(max = 50, message = "title should be smaller than 50 characters")
        String title,

        String slug,

        @NotEmpty(message = "content name should not be empty")
        @NotBlank(message = "content name should not be empty")
        String content,

        @NotEmpty(message = "summary name should not be empty")
        @NotBlank(message = "summary name should not be empty")
        String summary,

        List<String> tags,

        @NotNull
        Integer userId,

        @NotNull
        Integer days
) { }
