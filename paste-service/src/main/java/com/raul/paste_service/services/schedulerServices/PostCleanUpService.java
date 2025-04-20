package com.raul.paste_service.services.schedulerServices;

import com.raul.paste_service.dto.notification.EmailNotificationDto;
import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.SentPostNotification;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.SentPostNotificationRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import lombok.RequiredArgsConstructor;
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
@EnableAsync
public class PostCleanUpService {
    private final PostRepository postRepository;
    private final KafkaProducer kafkaProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}", initialDelayString = "${task.initial.delay.millis}")
    public void markAsDeletedExpiredPosts() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check and mark as deleted expired posts.");

        LocalDateTime now = LocalDateTime.now();

        try {
            List<Post> expiredPosts = postRepository.findAllExpiredPosts(now);

            if (expiredPosts.isEmpty()) {
                customLog.info(CUSTOM_LOG_MARKER, "No expired post found for mark as deleted.");
                return;
            }

            customLog.info(CUSTOM_LOG_MARKER, "Found {} expired posts for mark as deleted.", expiredPosts.size());

            try {
                sendNotification(expiredPosts);
                customLog.info(CUSTOM_LOG_MARKER, "Notifications sent for expired posts.");
            } catch (Exception e) {
                customLog.error(CUSTOM_LOG_MARKER, "Failed to send notifications for expired posts.", e);
            }

            postRepository.markAsDeletedExpiredPosts(now);

            customLog.info(CUSTOM_LOG_MARKER, "Successfully mark as deleted {} expired posts.", expiredPosts.size());
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred during expired post mark as deleted process.", e);
            throw new RuntimeException("Failed to mark up expired posts", e);
        }
    }

    @Scheduled(cron = "${task.cleanup.cron}")
    public void removeAllDeletedPosts() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check and remove deleted posts.");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            List<Post> oldDeletedPosts = postRepository.findPostsDeletedBefore(threshold);

            if (oldDeletedPosts.isEmpty()) {
                customLog.info(CUSTOM_LOG_MARKER, "No deleted post found for removal.");
                return;
            }

            customLog.info(CUSTOM_LOG_MARKER, "Found {} deleted posts for removal.", oldDeletedPosts.size());

            postRepository.deleteAll(oldDeletedPosts);

            customLog.info(CUSTOM_LOG_MARKER, "Successfully remove {} deleted posts.", oldDeletedPosts.size());
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred during deleted post removal process.", e);
            throw new RuntimeException("Failed to clean up deleted posts", e);
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
