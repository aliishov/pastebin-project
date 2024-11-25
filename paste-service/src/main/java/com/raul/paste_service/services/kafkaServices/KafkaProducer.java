package com.raul.paste_service.services.kafkaServices;

import com.raul.paste_service.dto.PostIdDto;
import com.raul.paste_service.dto.notification.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, PostIdDto> hashKafkaTemplate;
    private final KafkaTemplate<String, EmailNotificationDto> notificationKafkaTemplate;

    public void sendMessageToHashTopic(PostIdDto postIdDto) {
        log.info("sending postdto to hash service");
        hashKafkaTemplate.send("hash_topic", postIdDto);
    }

    public void sendMessageToNotificationTopic(EmailNotificationDto emailDto) {
        log.info("sending emailDto to notification service");
        notificationKafkaTemplate.send("email_notification_topic", emailDto);
    }
}
