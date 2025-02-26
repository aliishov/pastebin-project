package com.raul.paste_service.services.schedulerServices;

import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostIdDto;
import com.raul.paste_service.dto.notification.EmailNotificationDto;
import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.SentPostNotification;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.SentPostNotificationRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class PostCleanUpService {
    private final PostRepository postRepository;
    private final KafkaProducer kafkaProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final HashClient hashClient;

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}", initialDelayString = "${task.initial.delay.millis}")
    public void removeExpiredPosts() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check and remove expired posts.");

        LocalDateTime now = LocalDateTime.now();
        long beforeDelete = postRepository.count();

        try {
            List<Post> expiredPosts = postRepository.findAllExpiredPosts(now);

            if (expiredPosts.isEmpty()) {
                customLog.info(CUSTOM_LOG_MARKER, "No expired post found for removal.");
                return;
            }

            customLog.info(CUSTOM_LOG_MARKER, "Found {} expired posts for removal.", expiredPosts.size());

            try {
                sendNotification(expiredPosts);
                customLog.info(CUSTOM_LOG_MARKER, "Notifications sent for expired posts.");
            } catch (Exception e) {
                customLog.error(CUSTOM_LOG_MARKER, "Failed to send notifications for expired posts.", e);
            }

            try {
                sendDeleteRequestToHashService(expiredPosts);
                customLog.info(CUSTOM_LOG_MARKER, "Sent delete request to hash-service.");
            } catch (Exception e) {
                customLog.error(CUSTOM_LOG_MARKER, "Failed to send notifications for expired posts.", e);
            }

            postRepository.deleteExpiredPosts(now);
            long afterDelete = postRepository.count();

            customLog.info(CUSTOM_LOG_MARKER, "Successfully removed {} expired posts.", beforeDelete - afterDelete);
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred during expired post removal process.", e);
        }
    }

    public void sendDeleteRequestToHashService(List<Post> expiredPosts) {
        for (Post expiredPost : expiredPosts) {
            hashClient.deleteHash(new PostIdDto(expiredPost.getId()));
        }
    }

    @Async
    void sendNotification(List<Post> expiredPosts) {

        for (Post expiredPost : expiredPosts) {
            Map<String, String> placeholders = Map.of(
                    "post_title", expiredPost.getTitle()
            );

            SentPostNotification sentPostNotification = SentPostNotification.builder()
                    .postId(expiredPost.getId())
                    .notificationType(EmailNotificationSubject.POST_EXPIRATION_NOTIFICATION)
                    .sendAt(LocalDateTime.now())
                    .build();

            sentPostNotificationRepository.save(sentPostNotification);

            kafkaProducer.sendMessageToNotificationTopic(
                    new EmailNotificationDto(
                            expiredPost.getUserId(),
                            EmailNotificationSubject.POST_EXPIRATION_NOTIFICATION,
                            placeholders
                    )
            );
        }
    }
}
