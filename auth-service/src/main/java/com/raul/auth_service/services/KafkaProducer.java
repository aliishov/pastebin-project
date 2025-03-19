package com.raul.auth_service.services;

import com.raul.auth_service.dto.notification.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {
    private final KafkaTemplate<String, EmailNotificationDto> notificationKafkaTemplate;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    public void sendMessageToAuthNotificationTopic(EmailNotificationDto emailDto) {
        customLog.info(CUSTOM_LOG_MARKER, "sending emailDto to notification service");
        notificationKafkaTemplate.send("email_notification_topic", emailDto);
    }
}
