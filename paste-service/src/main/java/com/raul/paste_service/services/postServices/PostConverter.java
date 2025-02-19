package com.raul.paste_service.services.postServices;

import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostIdDto;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.models.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostConverter {

    private final HashClient hashClient;
    public Post convertToPost(PostRequestDto request, String slug) {
        return Post.builder()
                .title(request.title())
                .slug(slug)
                .content(request.content())
                .summary(request.summary())
                .userId(request.userId())
                .rating(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(request.days()))
                .isDeleted(false)
                .likesCount(0)
                .viewsCount(0)
                .indexedAt(LocalDateTime.now())
                .build();
    }

    public PostResponseDto convertToPostResponse(Post post) {
        return PostResponseDto.builder()
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .summary(post.getSummary())
                .tags(post.getTags())
                .userId(post.getUserId())
                .rating(post.getRating())
                .likesCount(post.getLikesCount())
                .viewsCount(post.getViewsCount())
                .expirationDate(post.getExpiresAt())
                .hash(hashClient.getHashByPostId(post.getId()).getBody())
                .build();
    }

    public PostIdDto convertToPostDto(Post post) {
        return new PostIdDto(
                post.getId()
        );
    }
}
