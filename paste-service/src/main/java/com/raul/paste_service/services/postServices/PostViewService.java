package com.raul.paste_service.services.postServices;

import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PostViewService {

    @Qualifier("stringRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private static final String VIEWS_IP_CACHE = "view:ip:";
    private static final String VIEWS_USERID_CACHE = "view:userid:";
    private static final Duration VIEW_TTL = Duration.ofMinutes(30);

    public void handleView(Integer postId, Integer userId, HttpServletRequest request) {

        if (userId != null && isAuthor(postId, userId)) return;

        String key;
        if (userId != null) {
            key = VIEWS_USERID_CACHE + userId + ":post:" + postId;
        } else {
            String ip = IpUtils.getClientIp(request);
            key = VIEWS_IP_CACHE + ip + ":post:" + postId;
        }

        Boolean alreadyViewed = redisTemplate.hasKey(key);

        if (Boolean.FALSE.equals(alreadyViewed)) {

            postRepository.incrementViews(postId);

            redisTemplate.opsForValue().set(key, "viewed", VIEW_TTL);
        }
    }

    private boolean isAuthor(Integer postId, Integer userId) {
        return postRepository.findAuthorId(postId)
                .map(authorId -> authorId.equals(userId))
                .orElse(false);
    }
}
