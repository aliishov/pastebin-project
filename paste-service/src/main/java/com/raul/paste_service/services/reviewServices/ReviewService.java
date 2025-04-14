package com.raul.paste_service.services.reviewServices;

import com.raul.paste_service.dto.review.ReviewRequestDto;
import com.raul.paste_service.dto.review.ReviewResponseDto;
import com.raul.paste_service.models.Review;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.ReviewRepository;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import com.raul.paste_service.utils.exceptions.ReviewNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PostRepository postRepository;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    /**
     * Adds a new review to a post.
     *
     * @param request DTO containing review details.
     * @return ResponseEntity with created ReviewResponseDto.
     */
    public ResponseEntity<ReviewResponseDto> addReview(ReviewRequestDto request) {

        customLog.info(CUSTOM_LOG_MARKER, "Adding review for post ID: {} by user ID: {}", request.postId(), request.userId());

        var post = postRepository.findByIdAndIsDeletedFalse(request.postId())
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "Post not found with ID: {}", request.postId());
                    return new PostNotFoundException("Post not found with ID: " + request.postId());
                });

        var review = Review.builder()
                .post(post)
                .userId(request.userId())
                .grade(request.grade())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        customLog.info(CUSTOM_LOG_MARKER, "Review saved with ID: {}", review.getId());

        ReviewResponseDto response = new ReviewResponseDto(
                review.getPost().getId(),
                review.getUserId(),
                review.getGrade(),
                review.getCreatedAt()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all reviews for a given post ID.
     *
     * @param postId ID of the post.
     * @return ResponseEntity containing a list of ReviewResponseDto.
     */
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByPostId(Integer postId) {
        customLog.info(CUSTOM_LOG_MARKER, "Fetching reviews for post ID: {}", postId);

        var reviews = reviewRepository.findByPostId(postId);

        if (reviews.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No reviews found for post ID: {}", postId);
            throw new ReviewNotFoundException("Reviews not found for post ID: " + postId);
        }

        List<ReviewResponseDto> responseList = reviews.stream()
                .map(review -> new ReviewResponseDto(
                        review.getPost().getId(),
                        review.getUserId(),
                        review.getGrade(),
                        review.getCreatedAt()
                )).collect(Collectors.toList());

        customLog.info(CUSTOM_LOG_MARKER, "Returning {} reviews for post ID: {}", responseList.size(), postId);
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    /**
     * Deletes a review by its ID.
     *
     * @param reviewId ID of the review to delete.
     * @return ResponseEntity with no content.
     */
    public ResponseEntity<Void> deleteReview(Integer reviewId) {
        customLog.info(CUSTOM_LOG_MARKER, "Attempting to delete review with ID: {}", reviewId);

        var review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "Review not found with ID: {}", reviewId);
                    return new ReviewNotFoundException("Review not found with ID: " + reviewId);
                });

        reviewRepository.delete(review);
        customLog.info(CUSTOM_LOG_MARKER, "Review with ID: {} deleted successfully", reviewId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
