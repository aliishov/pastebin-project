package com.raul.paste_service.services;

import com.raul.paste_service.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class PostCleanUpService {
    private final PostRepository postRepository;

    @Scheduled(fixedRateString = "${task.fixed.rate.millis}", initialDelayString = "${task.initial.delay.millis}")
    public void removeExpiredPosts() {
        log.info("Starting scheduled task to check and remove expired posts from the database.");

        long beforeDelete = postRepository.count();

        LocalDateTime now = LocalDateTime.now();
        postRepository.deleteExpiredPosts(now);

        long afterDelete = postRepository.count();

        if (beforeDelete - afterDelete == 0) {
            log.info("No expired posts found for removal.");
        } else {
            log.info("Removed {} expired posts from the database.", beforeDelete - afterDelete);

        }
    }
}
