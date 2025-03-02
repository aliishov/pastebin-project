package com.raul.hash_service.services;

import com.raul.hash_service.dto.HashResponseDto;
import com.raul.hash_service.dto.PostIdDto;
import com.raul.hash_service.models.Hash;
import com.raul.hash_service.repositories.HashRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashService {

    private final HashGenerationService hashGenerationService;
    private final HashRepository hashRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    public ResponseEntity<String> generateHash(PostIdDto request) {
        customLog.info(CUSTOM_LOG_MARKER, "Generating hash for post with id: {}", request.id());

        String hash = hashGenerationService.generateUniqueHash(request);

        customLog.info(CUSTOM_LOG_MARKER, "Hash '{}' successfully generated and saved for post with id: {}", hash, request.id());
        return new ResponseEntity<>(hash, HttpStatus.CREATED);
    }

    public ResponseEntity<Integer> getPostIdByHash(String hash) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find post ID by hash: {}", hash);

        Hash hashEntity = hashRepository.findByHash(hash)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "Hash not found: {}", hash);
                    return new IllegalArgumentException("Hash not found");
                });

        customLog.info(CUSTOM_LOG_MARKER, "Hash found for post ID: {}", hashEntity.getPostId());

        return new ResponseEntity<>(hashEntity.getPostId(), HttpStatus.OK);
    }

    public ResponseEntity<String> getHashByPostId(Integer postId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find hash by post ID: {}", postId);

        Hash hashEntity = hashRepository.findByPostId(postId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "Hash not found");
                    return new IllegalArgumentException("Hash not found");
                });

        customLog.info(CUSTOM_LOG_MARKER, "Hash found for post ID: {}", hashEntity.getPostId());

        return new ResponseEntity<>(hashEntity.getHash(), HttpStatus.OK);
    }

    public ResponseEntity<Void> deleteHash(PostIdDto request) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete hash by post ID: {}", request.id());

        hashRepository.deleteHash(request.id());

        customLog.info(CUSTOM_LOG_MARKER, "Hash deleted for post ID: {}", request.id());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<List<HashResponseDto>> restoreAllHashesByPostsId(List<Integer> postIds) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to restore hashes for post IDs: {}", postIds);

        if (postIds == null || postIds.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No post IDs provided for hash restoration.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Hash> deletedHashes = hashRepository.findAllByPostIdInAndIsDeletedTrue(postIds);

        if (deletedHashes.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No deleted hashes found for given post IDs.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        deletedHashes.forEach(hash -> hash.setIsDeleted(false));

        hashRepository.saveAll(deletedHashes);

        List<HashResponseDto> restoredHashes = deletedHashes.stream()
                .map(hash -> new HashResponseDto(hash.getHash(), hash.getPostId()))
                .collect(Collectors.toList());

        customLog.info(CUSTOM_LOG_MARKER, "Successfully restored {} hashes", restoredHashes.size());

        return new ResponseEntity<>(restoredHashes, HttpStatus.OK);
    }
}
