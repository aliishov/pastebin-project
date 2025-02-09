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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class PopularPostCacheManager {

    private final PostRepository postRepository;
    private final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    private final ObjectMapper mapper;
    private static final long TTL = 10;
    private final PostConverter postConverter;
    private final KafkaProducer kafkaNotificationProducer;
    private final SentPostNotificationRepository sentPostNotificationRepository;

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}")
    public void updatePopularPostInRedis() {
        log.info("Starting scheduled task to check post views and put popular posts in to Redis.");

        List<Post> popularPosts = postRepository.findAllByViewsCount();

        for (Post popularPost : popularPosts) {
            putInRedis(popularPost);

            if (!isNotificationSend(popularPost.getId())) {

                log.info("Creating new sentPostNotification");

                SentPostNotification sentPostNotification = SentPostNotification.builder()
                        .postId(popularPost.getId())
                        .sendAt(LocalDateTime.now())
                        .build();

                sentPostNotificationRepository.save(sentPostNotification);

                Map<String, String> placeholders = Map.of(
                        "post_title", popularPost.getTitle()
                );

                kafkaNotificationProducer.sendMessageToNotificationTopic(
                        new EmailNotificationDto(
                                popularPost.getUserId(),
                                EmailNotificationSubject.POPULAR_POST_NOTIFICATION,
                                placeholders
                        )
                );
            }
        }
    }

    private void putInRedis(Post post) {

        var postResponse = postConverter.convertToPostResponse(post);

        try (Jedis jedis = jedisPool.getResource()) {
            String postJson = mapper.writeValueAsString(postResponse);
            String key = "post:%d".formatted(post.getId());
            jedis.setex(key, TTL, postJson);
            log.info("Post with ID {} cached in Redis.", post.getId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotificationSend(Integer postId) {
        log.info("Check sentNotification for post with ID: {}", postId);

        return sentPostNotificationRepository.findByPostId(postId).isPresent();
    }
}
