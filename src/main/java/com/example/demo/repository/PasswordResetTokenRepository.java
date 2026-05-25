package com.example.demo.repository;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetToken t where t.user.id = :userId and t.id <> :keepId")
    void deleteOtherTokensForUser(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
