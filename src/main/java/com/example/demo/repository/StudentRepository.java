package com.example.demo.repository;

import com.example.demo.entity.Student;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByUser(User user);

    Optional<Student> findByPublicKey(String publicKey);

    List<Student> findBySectionId(Long sectionId);

    long countBySectionId(Long sectionId);
}
