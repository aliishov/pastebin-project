package com.raul.paste_service.services.postServices;

import com.raul.paste_service.dto.post.PostIndexDto;
import com.raul.paste_service.dto.post.PostRequestDto;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.dto.tag.TagResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import com.raul.paste_service.repositories.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostConverter {

    private final PostLikeRepository postLikeRepository;

    public Post convertToPost(PostRequestDto request, Integer userId) {
        return Post.builder()
                .title(request.title())
                .slug(generateUniqueSlug(request.slug()))
                .content(request.content())
                .summary(request.summary())
                .tags(new HashSet<>())
                .userId(userId)
                .rating(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(request.days()))
                .isDeleted(false)
                .likes(new ArrayList<>())
                .viewsCount(0)
                .indexedAt(LocalDateTime.now())
                .deletedAt(null)
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
                .likesCount(postLikeRepository.countLikesByPostId(post.getId()))
                .viewsCount(post.getViewsCount())
                .expirationDate(post.getExpiresAt())
                .hash(null)
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
                postLikeRepository.countLikesByPostId(post.getId()),
                post.getViewsCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getExpiresAt(),
                post.getIsDeleted()
        );
    }

    private String generateUniqueSlug(String title) {
        String suffix = UUID.randomUUID().toString();

        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z\\d\\s]", "")
                .replaceAll("\\s+", "-");

        int maxSlugLength = 100 - suffix.length() - 1;
        if (baseSlug.length() > maxSlugLength) {
            baseSlug = baseSlug.substring(0, maxSlugLength);
        }

        return baseSlug + "-" + suffix;
    }
}
