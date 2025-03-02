package com.example.user_service.repository;

import com.example.user_service.model.User;
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
public interface UserRepository extends JpaRepository<User, Integer> {

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true, u.deletedAt = :deletedAt WHERE u.id = :userId")
    void markAsDeletedById(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);

    @Query("SELECT u FROM User u WHERE u.isDeleted = true AND u.deletedAt <= :threshold")
    List<User> findUsersDeletedBefore(@Param("threshold") LocalDateTime threshold);

    Optional<User> findByEmail(String email);
}
