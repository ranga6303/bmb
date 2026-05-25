package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserAndRevokedFalse(User user);

    @Modifying(flushAutomatically = true)
    @Query("update UserSession s set s.revoked = true where s.user.id = :userId and s.revoked = false")
    int revokeAllActiveByUserId(@Param("userId") Long userId);

    Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String hash);

    void deleteByUser(User user);
}
