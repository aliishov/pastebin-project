package com.raul.paste_service.dto.notification;

import java.util.Map;

public record EmailNotificationDto(
        Integer to,
        EmailNotificationSubject subject,
        Map<String, String> placeholders
) {
}
