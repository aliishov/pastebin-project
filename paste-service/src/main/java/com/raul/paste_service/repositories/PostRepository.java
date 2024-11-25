package com.raul.paste_service.repositories;

import com.raul.paste_service.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
//    @Query(value = "SELECT p.id, p.content, p.user_id, p.expires_at, h.hash " +
//            "FROM posts p JOIN hashes h ON p.id = h.post_id",
//            nativeQuery = true)
//    List<Object[]> findAllPostsWithHashes();

    @Transactional
    @Modifying
    @Query("DELETE FROM Post p WHERE p.expiresAt <= :now")
    void deleteExpiredPosts(LocalDateTime now);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikes(Integer postId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViews(Integer postId);

    @Query("SELECT p FROM Post p WHERE p.viewsCount >= 1000")
    List<Post> findAllByViewsCount();

    @Query("SELECT p FROM Post p WHERE p.expiresAt <= :now")
    List<Post> findAllExpiredPosts(LocalDateTime now);
}
