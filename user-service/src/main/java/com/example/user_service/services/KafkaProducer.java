package com.example.user_service.services;

import com.example.user_service.dto.notification.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {
    private final KafkaTemplate<String, EmailNotificationDto> notificationKafkaTemplate;

    public void sendMessageToAuthNotificationTopic(EmailNotificationDto emailDto) {
        log.info("sending emailDto to notification service");
        notificationKafkaTemplate.send("email_notification_topic", emailDto);
    }
}
