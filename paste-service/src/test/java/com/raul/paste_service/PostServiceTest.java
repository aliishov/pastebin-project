package com.raul.paste_service;

import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.services.postServices.PostConverter;
import com.raul.paste_service.services.postServices.PostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private RedisTemplate<String, PostResponseDto> redisTemplate;

    @Mock
    private ValueOperations<String, PostResponseDto> valueOperations;

    @Mock
    private PostRepository postRepository;

    @Mock
    private HashClient hashClient;

    @Mock
    private PostConverter converter;

    @InjectMocks
    private PostService postService; // тот класс, где находится метод

    private static final String HASH = "abc123";
    private static final String REDIS_KEY = "posts::42";

    @Test
    void shouldReturnPostFromCache() {
        PostResponseDto cachedPost = new PostResponseDto();
        cachedPost.setTitle("Cached Post");
        cachedPost.setHash(HASH);

        Mockito.when(hashClient.getPostIdByHash(HASH)).thenReturn(ResponseEntity.ok(42));
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get(REDIS_KEY)).thenReturn(cachedPost);

        ResponseEntity<PostResponseDto> response = postService.getPostByHash(HASH);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("Cached Post", response.getBody().getTitle());

        Mockito.verify(postRepository, Mockito.never()).findByIdAndIsDeletedFalse(Mockito.anyInt());
    }

    @Test
    void shouldFetchPostWhenNotInCache() {
        Integer postId = 42;
        Post post = new Post();
        post.setId(postId);
        post.setTitle("DB Post");

        PostResponseDto responseDto = new PostResponseDto();
        responseDto.setTitle("DB Post");
        responseDto.setHash(HASH);

        Mockito.when(hashClient.getPostIdByHash(HASH)).thenReturn(ResponseEntity.ok(postId));
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        Mockito.when(postRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(post));
        Mockito.when(converter.convertToPostResponse(post)).thenReturn(responseDto);

        ResponseEntity<PostResponseDto> response = postService.getPostByHash(HASH);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("DB Post", response.getBody().getTitle());

        Mockito.verify(postRepository).findByIdAndIsDeletedFalse(postId);
        Mockito.verify(converter).convertToPostResponse(post);
    }
}
