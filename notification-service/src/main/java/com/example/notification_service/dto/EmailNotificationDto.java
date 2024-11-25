package com.example.notification_service.dto;

import jakarta.validation.constraints.NotNull;

public record EmailNotificationDto(
        @NotNull
        Integer to,
        EmailNotificationSubject subject
) {
}
