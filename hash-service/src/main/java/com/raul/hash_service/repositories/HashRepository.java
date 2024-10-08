package com.raul.hash_service.repositories;

import com.raul.hash_service.models.Hash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HashRepository extends JpaRepository<Hash, Integer> {

    Optional<Hash> findByHash(String hash);
    Optional<Hash> findByPostId(Integer postId);
}
