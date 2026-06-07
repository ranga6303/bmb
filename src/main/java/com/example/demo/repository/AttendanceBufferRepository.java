package com.example.demo.repository;

import com.example.demo.entity.AttendanceBuffer;
import com.example.demo.entity.Session;
import com.example.demo.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceBufferRepository extends JpaRepository<AttendanceBuffer, Long> {
    boolean existsBySessionAndStudent(Session session, Student student);

    List<AttendanceBuffer> findBySession(Session session);

    void deleteBySession(Session session);
}
