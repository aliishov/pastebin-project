package com.raul.paste_service.dto.post;

import jakarta.validation.constraints.PositiveOrZero;

public record RestorePostDto(
        @PositiveOrZero(message = "UserId should be greater than or equal to 0")
        Integer days
) {
}
