package com.raul.paste_service.services.schedulerServices;

import com.raul.paste_service.dto.notification.EmailNotificationDto;
import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.SentPostNotification;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.SentPostNotificationRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.services.postServices.PostConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@EnableAsync
public class PopularPostCacheManager {

    private final PostRepository postRepository;
    private static final long TTL = 60;
    private final PostConverter postConverter;
    private final KafkaProducer kafkaNotificationProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final RedisTemplate<String, PostResponseDto> redisTemplate;

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}")
    public void updatePopularPostInRedis() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check post views and put popular posts in to Redis.");

        List<Post> popularPosts = postRepository.findAllByViewsCount();
        if (popularPosts.isEmpty()) {
            customLog.info(CUSTOM_LOG_MARKER, "No popular posts found for caching.");
            return;
        }

        Set<Integer> notifiedPostIds = sentPostNotificationRepository.findAllNotifiedPostIds();

        for (Post popularPost : popularPosts) {
            try {
                putInRedis(popularPost);
            } catch (Exception e) {
                customLog.error(CUSTOM_LOG_MARKER, "Failed to cache post with ID {} in Redis.", popularPost.getId(), e);
                continue;
            }

            if (!notifiedPostIds.contains(popularPost.getId())) {
                sendNotification(popularPost);
            }
        }
    }

    private void putInRedis(Post post) {
        String key = "post:" + post.getId();

        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(hasKey)) {
            customLog.info(CUSTOM_LOG_MARKER, "Post with ID {} already exists in Redis. Skipping.", post.getId());
            return;
        }

        PostResponseDto postResponse = postConverter.convertToPostResponse(post);
        redisTemplate.opsForValue().set(key, postResponse, TTL, TimeUnit.SECONDS);

        customLog.info(CUSTOM_LOG_MARKER, "Post with ID {} cached in Redis.", post.getId());
    }

    private void sendNotification(Post post) {
        customLog.info(CUSTOM_LOG_MARKER, "Creating new sentPostNotification for post ID: {}", post.getId());

        SentPostNotification sentPostNotification = SentPostNotification.builder()
                .postId(post.getId())
                .notificationType(EmailNotificationSubject.POPULAR_POST_NOTIFICATION)
                .sendAt(LocalDateTime.now())
                .build();

        sentPostNotificationRepository.save(sentPostNotification);

        Map<String, String> placeholders = Map.of(
                "post_title", post.getTitle()
        );

        kafkaNotificationProducer.sendMessageToNotificationTopic(
                new EmailNotificationDto(
                        post.getUserId(),
                        EmailNotificationSubject.POPULAR_POST_NOTIFICATION,
                        placeholders
                )
        );

        customLog.info(CUSTOM_LOG_MARKER, "Notification sent for post ID: {}", post.getId());
    }
}
