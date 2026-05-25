package com.example.demo.repository;

import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByTeacherId(String teacherId);

    Optional<Teacher> findByUser(User user);

    @Query("SELECT COUNT(t) > 0 FROM Teacher t JOIN t.mappedSections s WHERE s = :section AND t.user.role = :role")
    boolean existsBySectionAndRole(@Param("section") Section section, @Param("role") Role role);
}
