package com.raul.paste_service.repositories;

import com.raul.paste_service.models.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

    @Query("SELECT COUNT(l) FROM PostLike l WHERE l.post.id = :postId")
    Integer countLikesByPostId(@Param("postId") Integer postId);

    Boolean existsByPost_IdAndUserId(Integer postId, Integer userId);

    Optional<PostLike> findByPost_IdAndUserId(Integer postId, Integer userId);

    List<PostLike> findByUserId(Integer userId);

    @Query(value =
            "SELECT MAX(likeCount) " +
            "FROM (SELECT COUNT(p.id) AS likeCount " +
            "FROM post_likes p GROUP BY p.post_id) AS likeCounts",
            nativeQuery = true)
    int findMaxLikes();
}
