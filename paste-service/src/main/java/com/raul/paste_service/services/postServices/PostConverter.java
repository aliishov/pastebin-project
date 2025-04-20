package com.raul.paste_service.services.postServices;

import com.raul.paste_service.dto.post.PostIndexDto;
import com.raul.paste_service.dto.post.PostRequestDto;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.dto.tag.TagResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import com.raul.paste_service.repositories.PostLikeRepository;
import com.raul.paste_service.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostConverter {

    private final PostLikeRepository postLikeRepository;
    private final HashGenerationService hashGenerationService;
    private final PostRepository postRepository;

    public Post convertToPost(PostRequestDto request, Integer userId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = request.days() != null
                ? now.plusDays(request.days())
                : null;

        return Post.builder()
                .title(request.title())
                .slug(generateUniqueSlug(request.slug()))
                .content(request.content())
                .summary(request.summary())
                .tags(new HashSet<>())
                .hash(hashGenerationService.generateUniqueHash())
                .userId(userId)
                .rating(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
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
                .hash(post.getHash())
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
                post.getHash(),
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
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String baseSlug = normalized.toLowerCase()
                .replaceAll("[^a-z\\d\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        int suffix = 1;
        String slug;

        int maxSlugLength = 100;

        while (true) {
            String suffixStr = "-" + suffix;
            int maxBaseLength = maxSlugLength - suffixStr.length();
            String trimmedBase = baseSlug.length() > maxBaseLength ? baseSlug.substring(0, maxBaseLength) : baseSlug;
            slug = trimmedBase + suffixStr;

            if (!postRepository.existsBySlug(slug)) {
                break;
            }

            suffix++;
        }

        return slug;
    }
}
