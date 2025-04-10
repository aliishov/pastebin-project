package com.raul.paste_service.repositories;

import com.raul.paste_service.models.Post;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.isDeleted = true, p.deletedAt = :now WHERE p.expiresAt <= :now")
    void markAsDeletedExpiredPosts(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViews(@Param("postId") Integer postId);

    @Query("SELECT p FROM Post p WHERE p.viewsCount >= 1000 AND p.isDeleted = false")
    List<Post> findAllByViewsCount();

    @Query("SELECT p FROM Post p WHERE p.expiresAt <= :now AND p.isDeleted = false")
    List<Post> findAllExpiredPosts(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Post p WHERE p.slug <= :slug AND p.isDeleted = false")
    Optional<Post> findPostBySlug(@Param("slug") String slug);

    @Override
    @Query("SELECT p FROM Post p WHERE p.id <= :postId AND p.isDeleted = false")
    Optional<Post> findById(@Param("postId") @NotNull Integer postId);

    @Query("SELECT p FROM Post p WHERE p.userId = :userId")
    List<Post> findAllByUserId(@Param("userId") Integer userId);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = true AND p.deletedAt <= :threshold")
    List<Post> findPostsDeletedBefore(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false")
    List<Post> findAll();

//    @Query("SELECT COALESCE(MAX(p.likesCount), 0) FROM Post p")
//    int findMaxLikes();

    @Query("SELECT COALESCE(MAX(p.viewsCount), 0) FROM Post p")
    int findMaxViews();

    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.isDeleted = false")
    List<Post> findByUserId(@Param("userId") Integer userId);

    List<Post> findByUserIdAndIsDeletedTrue(Integer userId);
}
