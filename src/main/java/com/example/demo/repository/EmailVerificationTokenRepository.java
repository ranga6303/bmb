package com.example.demo.repository;

import com.example.demo.entity.EmailVerificationToken;
import com.example.demo.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByTokenHashAndUsedFalse(String tokenHash);

    void deleteByUser(User user);

    void deleteByCollegeId(String collegeId);

    @Transactional
    @Modifying
    @Query("delete from EmailVerificationToken t where t.user.id = :userId and t.id <> :keepId")
    void deleteOtherTokensForUser(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
