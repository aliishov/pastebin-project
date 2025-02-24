package com.raul.paste_service.services.postServices;

import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostIndexDto;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.dto.TagResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                .tags(new HashSet<>())
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
                .tags(convertToTagResponse(post.getTags()))
                .userId(post.getUserId())
                .rating(post.getRating())
                .likesCount(post.getLikesCount())
                .viewsCount(post.getViewsCount())
                .expirationDate(post.getExpiresAt())
                .hash(hashClient.getHashByPostId(post.getId()).getBody())
                .build();
    }

    public List<TagResponseDto> convertToTagResponse(Set<Tag> tags) {
        return tags.stream()
                .map(tag -> new TagResponseDto(tag.getName()))
                .collect(Collectors.toList());
    }

    public PostIndexDto convertToPostIndex(Post post) {
        return new PostIndexDto(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getSummary(),
                post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList()),
                post.getUserId(),
                post.getRating(),
                post.getLikesCount(),
                post.getViewsCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getExpiresAt(),
                post.getIsDeleted()
        );
    }
}
