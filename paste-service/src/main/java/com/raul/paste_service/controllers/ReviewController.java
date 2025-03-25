package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.review.ReviewRequestDto;
import com.raul.paste_service.dto.review.ReviewResponseDto;
import com.raul.paste_service.services.reviewServices.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Review Controller", description = "Manages reviews in the Paste Service")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Add new review", description = "Add new review and returns the created review details.")
    @ApiResponse(responseCode = "201", description = "Review added successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponseDto.class)))
    @PostMapping
    public ResponseEntity<ReviewResponseDto> addReview(@RequestBody @Valid ReviewRequestDto request) {
        return reviewService.addReview(request);
    }

    @Operation(summary = "Get reviews by post ID", description = "Retrieves a reviews by post ID.")
    @ApiResponse(responseCode = "200", description = "Reviews found",
            content = @Content(schema = @Schema(implementation = ReviewResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Reviews not found")
    @GetMapping("/{postId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByPostId(@PathVariable Integer postId) {
        return reviewService.getReviewsByPostId(postId);
    }

    @Operation(summary = "Delete a review", description = "Delete a review")
    @ApiResponse(responseCode = "204", description = "Review deleted")
    @ApiResponse(responseCode = "404", description = "Review not found")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer reviewId) {
        return reviewService.deleteReview(reviewId);
    }
}
