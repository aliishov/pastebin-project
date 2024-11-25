package com.raul.paste_service.services.schedulerServices;

import com.raul.paste_service.dto.notification.EmailNotificationDto;
import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class PostCleanUpService {
    private final PostRepository postRepository;
    private final KafkaProducer kafkaNotificationProducer;

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}", initialDelayString = "${task.initial.delay.millis}")
    public void removeExpiredPosts() {
        log.info("Starting scheduled task to check and remove expired posts from the database.");

        long beforeDelete = postRepository.count();

        LocalDateTime now = LocalDateTime.now();

        List<Post> expiredPosts = postRepository.findAllExpiredPosts(now);

        sendNotification(expiredPosts);

        postRepository.deleteExpiredPosts(now);

        long afterDelete = postRepository.count();

        if (beforeDelete - afterDelete == 0) {
            log.info("No expired posts found for removal.");
        } else {
            log.info("Removed {} expired posts from the database.", beforeDelete - afterDelete);

        }
    }

    @Async
    void sendNotification(List<Post> expiredPosts) {

        for (Post expiredPost : expiredPosts) {
            kafkaNotificationProducer.sendMessageToNotificationTopic(
                    new EmailNotificationDto(
                            expiredPost.getId(),
                            EmailNotificationSubject.POST_EXPIRATION_NOTIFICATION
                    )
            );
        }
    }
}
