package com.example.demo.repository;

import com.example.demo.entity.Room;
import com.example.demo.entity.Section;
import com.example.demo.entity.Session;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    boolean existsByTeacherAndStatus(Teacher teacher, SessionStatus status);

    boolean existsBySectionAndStatus(Section section, SessionStatus status);

    boolean existsByRoomAndStatus(Room room, SessionStatus status);

    boolean existsBySessionCode(String sessionCode);

    boolean existsBySubjectAndSectionAndSessionDate(Subject subject, Section section, LocalDate sessionDate);

    Optional<Session> findBySessionCodeAndStatus(String sessionCode, SessionStatus status);

    Optional<Session> findBySectionAndStatus(Section section, SessionStatus status);

    Optional<Session> findByTeacherUserAndStatusIn(User teacher, List<SessionStatus> statuses);

    Optional<Session> findByTeacherAndStatusIn(Teacher teacher, List<SessionStatus> statuses);

    long countBySubjectAndSectionAndStatus(Subject subject, Section section, SessionStatus status);

    List<Session> findByStatusAndExpiryTimeBefore(SessionStatus status, LocalDateTime expiryTime);

    List<Session> findByStatusAndLockedAtBefore(SessionStatus status, LocalDateTime lockedAt);
}
