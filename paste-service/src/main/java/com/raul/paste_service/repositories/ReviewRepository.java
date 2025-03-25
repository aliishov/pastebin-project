package com.raul.paste_service.repositories;

import com.raul.paste_service.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.OptionalDouble;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByPostId(Integer postId);

    @Query("SELECT AVG(r.grade) FROM Review r WHERE r.post.id = :postId")
    OptionalDouble findAverageGradeByPostId(@Param("postId") Integer postId);

}
