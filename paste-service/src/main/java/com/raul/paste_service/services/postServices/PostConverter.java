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
    public Post convertToPost(PostRequestDto request) {
        return Post.builder()
                .content(request.content())
                .userId(request.userId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(request.days()))
                .isDeleted(false)
                .likesCount(0)
                .viewsCount(0)
                .build();
    }

    public PostResponseDto convertToPostResponse(Post post) {
        return new PostResponseDto(
                post.getContent(),
                post.getUserId(),
                post.getLikesCount(),
                post.getViewsCount(),
                post.getExpiresAt(),
                hashClient.getHashByPostId(post.getId()).getBody()
        );
    }

    public PostIdDto convertToPostDto(Post post) {
        return new PostIdDto(
                post.getId()
        );
    }
}
