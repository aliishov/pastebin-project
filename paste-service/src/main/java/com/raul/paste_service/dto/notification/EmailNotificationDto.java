package com.raul.paste_service.dto.notification;

public record EmailNotificationDto(
        Integer to,
        EmailNotificationSubject subject
) {
}
