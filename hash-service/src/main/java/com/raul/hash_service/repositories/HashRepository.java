package com.raul.hash_service.repositories;

import com.raul.hash_service.models.Hash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HashRepository extends JpaRepository<Hash, Integer> {

    @Query("SELECT h FROM Hash h WHERE h.hash = :hash AND h.isDeleted = false")
    Optional<Hash> findByHash(String hash);

    @Query("SELECT h FROM Hash h WHERE h.postId = :postId AND h.isDeleted = false")
    Optional<Hash> findByPostId(Integer postId);

    @Transactional
    @Modifying
    @Query("UPDATE Hash h SET h.isDeleted = true WHERE h.postId <= :postId")
    void deleteHash(Integer postId);

    List<Hash> findAllByPostIdInAndIsDeletedTrue(List<Integer> postIds);
}
