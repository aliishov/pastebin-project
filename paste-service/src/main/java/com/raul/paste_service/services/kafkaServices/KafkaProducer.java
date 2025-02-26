package com.raul.paste_service.services.kafkaServices;

import com.raul.paste_service.dto.PostIndexDto;
import com.raul.paste_service.dto.notification.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class KafkaProducer {
    private final KafkaTemplate<String, EmailNotificationDto> notificationKafkaTemplate;
    private final KafkaTemplate<String, PostIndexDto> postIndexKafkaTemplate;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Async
    public void sendMessageToNotificationTopic(EmailNotificationDto emailDto) {
        customLog.info(CUSTOM_LOG_MARKER, "Sending emailDto to notification-service");
        notificationKafkaTemplate.send("email_notification_topic", emailDto);
    }

    @Async
    public void sendMessageToPostIndexTopic(PostIndexDto postIndexDto) {
        customLog.info(CUSTOM_LOG_MARKER, "Sending postIndexDto to search-service");
        postIndexKafkaTemplate.send("post_index_topic", postIndexDto);
    }
}
