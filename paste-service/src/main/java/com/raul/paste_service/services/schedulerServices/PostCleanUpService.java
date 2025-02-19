package com.raul.paste_service.services.schedulerServices;

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
    private final KafkaProducer kafkaNotificationProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}", initialDelayString = "${task.initial.delay.millis}")
    public void removeExpiredPosts() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check and remove expired posts from the database.");

        long beforeDelete = postRepository.count();

        LocalDateTime now = LocalDateTime.now();

        List<Post> expiredPosts = postRepository.findAllExpiredPosts(now);

        sendNotification(expiredPosts);

        postRepository.deleteExpiredPosts(now);

        long afterDelete = postRepository.count();

        if (beforeDelete - afterDelete == 0) {
            customLog.info(CUSTOM_LOG_MARKER, "No expired posts found for removal.");
        } else {
            customLog.info(CUSTOM_LOG_MARKER, "Removed {} expired posts from the database.", beforeDelete - afterDelete);
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

            kafkaNotificationProducer.sendMessageToNotificationTopic(
                    new EmailNotificationDto(
                            expiredPost.getUserId(),
                            EmailNotificationSubject.POST_EXPIRATION_NOTIFICATION,
                            placeholders
                    )
            );
        }
    }
}
