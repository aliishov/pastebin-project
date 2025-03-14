package com.example.notification_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record EmailNotificationDto(
        @NotNull
        Integer to,

        @NotNull
        EmailNotificationSubject subject,

        @NotNull
        Map<String, String> placeholders
) {
}
