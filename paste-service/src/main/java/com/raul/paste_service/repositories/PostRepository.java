package com.raul.paste_service.repositories;

import com.raul.paste_service.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.expiresAt <= :now")
    void deleteExpiredPosts(LocalDateTime now);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikes(Integer postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViews(Integer postId);

    @Query("SELECT p FROM Post p WHERE p.viewsCount >= 1000 AND p.isDeleted = false")
    List<Post> findAllByViewsCount();

    @Query("SELECT p FROM Post p WHERE p.expiresAt <= :now AND p.isDeleted = false")
    List<Post> findAllExpiredPosts(LocalDateTime now);

    @Query("SELECT p FROM Post p WHERE p.slug <= :slug AND p.isDeleted = false")
    Optional<Post> findPostBySlug(String slug);

    @Override
    @Query("SELECT p FROM Post p WHERE p.id <= :postId AND p.isDeleted = false")
    Optional<Post> findById(Integer postId);

    @Query("SELECT p FROM Post p WHERE p.userId = :userId")
    List<Post> findAllByUserId(Integer userId);
}
