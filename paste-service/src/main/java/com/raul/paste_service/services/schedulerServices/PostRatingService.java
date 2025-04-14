package com.raul.paste_service.services.schedulerServices;

import com.raul.paste_service.models.Post;
import com.raul.paste_service.repositories.PostLikeRepository;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostRatingService {

    private final ReviewRepository reviewRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Scheduled(cron = "${task.rating.cron}")
    @Transactional
    public void updatePostRatings() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting post rating update...");

        int maxLikes = postLikeRepository.findMaxLikes();
        int maxViews = postRepository.findMaxViews();

        List<Post> posts = postRepository.findAllByIsDeletedFalse();

        for (Post post : posts) {
            int postLikesCount = postLikeRepository.countLikesByPostId(post.getId());
            int postViewsCount = post.getViewsCount();

            double averageGrade = reviewRepository.findAverageGradeByPostId(post.getId()).orElse(1.0);

            double normalizedLikes = maxLikes > 0 ? 1 + ((double) postLikesCount / maxLikes) * 4 : 1;
            double normalizedViews = maxViews > 0 ? 1 + ((double) postViewsCount / maxViews) * 4 : 1;

            int newRating = (int) Math.round(0.7 * averageGrade + 0.2 * normalizedLikes + 0.1 * normalizedViews);
            newRating = Math.max(1, Math.min(5, newRating));

            post.setRating(newRating);
            customLog.info(CUSTOM_LOG_MARKER, "Updated post ID {}: rating = {}", post.getId(), newRating);
        }

        postRepository.saveAll(posts);
        customLog.info(CUSTOM_LOG_MARKER, "Post rating update completed.");
    }
}
