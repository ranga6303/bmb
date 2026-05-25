package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserAndRevokedFalse(User user);

    Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String hash);

    void deleteByUser(User user);
}
