package com.raul.paste_service.services.schedulerServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.paste_service.dto.notification.EmailNotificationDto;
import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.SentPostNotification;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.SentPostNotificationRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.services.postServices.PostConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class PopularPostCacheManager {

    private final PostRepository postRepository;
    private final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    private final ObjectMapper mapper;
    private static final long TTL = 3600;
    private final PostConverter postConverter;
    private final KafkaProducer kafkaNotificationProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

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
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "post:%d".formatted(post.getId());

            if (jedis.exists(key)) {
                customLog.info(CUSTOM_LOG_MARKER, "Post with ID {} already exists in Redis. Skipping.", post.getId());
                return;
            }

            var postResponse = postConverter.convertToPostResponse(post);
            String postJson = mapper.writeValueAsString(postResponse);
            jedis.setex(key, TTL, postJson);

            customLog.info(CUSTOM_LOG_MARKER, "Post with ID {} cached in Redis.", post.getId());
        } catch (JsonProcessingException e) {
            customLog.error(CUSTOM_LOG_MARKER, "Failed to serialize post with ID {}.", post.getId(), e);
            throw new RuntimeException(e);
        }
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
