package com.raul.paste_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

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

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}")
    public void updatePopularPostInRedis() {
        log.info("Starting scheduled task to check post views and put popular posts in to Redis.");

        List<Post> popularPosts = postRepository.findAllByViewsCount();

        for (Post popularPost : popularPosts) {
            putInRedis(popularPost);
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
}
