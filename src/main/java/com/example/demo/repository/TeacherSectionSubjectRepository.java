package com.example.demo.repository;

import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.TeacherSectionSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherSectionSubjectRepository extends JpaRepository<TeacherSectionSubject, Long> {
    boolean existsByTeacherAndSectionAndSubject(Teacher teacher, Section section, Subject subject);

    List<TeacherSectionSubject> findByTeacher(Teacher teacher);

    void deleteByTeacherAndSectionAndSubject(Teacher teacher, Section section, Subject subject);

    boolean existsByTeacherAndSection(Teacher teacher, Section section);
}
