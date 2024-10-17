package com.raul.hash_service.services;

import com.raul.hash_service.dto.PostDto;
import com.raul.hash_service.models.Hash;
import com.raul.hash_service.repositories.HashRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HashGenerationService {

    private final HashRepository hashRepository;
    private static final int HASH_LENGTH = 8;
    private final SecureRandom sRandom = new SecureRandom();
    private final Set<String> hashesSet = new HashSet<>();

    /**
     * Generation unique hash for paste
     *
     * @param postDto Post, for which the hash is generated.
     * @return Generated hash.
     */
    public String generateUniqueHash(PostDto postDto) {
        String hash;
        do {
            hash = generateRandomHash();
        } while (hashExists(hash));

        hashesSet.add(hash);

        Hash hashEntity = Hash.builder()
                .hash(hash)
                .postId(postDto.id())
                .createdAt(LocalDateTime.now())
                .build();

        hashRepository.save(hashEntity);

        return hash;
    }

    /**
     * Generate a random hash using Base64.
     *
     * @return Random hash.
     */
    public String generateRandomHash() {
        byte[] randomBytes = new byte[HASH_LENGTH];
        sRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Check if such hash already exists in the database.
     *
     * @param hash Hash to check.
     * @return true if hash exists, otherwise false.
     */
    public boolean hashExists(String hash) {
        return hashesSet.contains(hash) || hashRepository.findByHash(hash).isPresent();
    }
}
