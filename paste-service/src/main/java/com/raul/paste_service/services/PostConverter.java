package com.raul.paste_service.services;

import com.raul.paste_service.dto.PostDto;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.models.Post;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostConverter {
    public Post convertToPost(PostRequestDto request) {
        return Post.builder()
                .content(request.content())
                .userId(request.userId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(request.days()))
                .isDeleted(false)
                .build();
    }

    public PostResponseDto convertToPostResponse(Post post) {
        return new PostResponseDto(
                post.getContent(),
                post.getUserId(),
                post.getExpiresAt()
        );
    }

    public PostDto convertToPostDto(Post post) {
        return new PostDto(
                post.getId(),
                post.getContent(),
                post.getUserId(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getExpiresAt(),
                post.getIsDeleted()
        );
    }
}
