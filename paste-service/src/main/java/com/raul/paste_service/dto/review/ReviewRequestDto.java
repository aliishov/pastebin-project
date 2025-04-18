package com.raul.paste_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequestDto(

        @NotNull(message = "Post ID is required")
        Integer postId,

        @NotNull
        @Min(value = 1, message = "Minimum value should be greater than 0")
        @Max(value = 5, message = "Max value should be 5")
        Integer grade
) {
}
