package com.example.demo.repository;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.AttendanceStatus;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Student;
import com.example.demo.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    long countByStudentAndSessionSubjectAndSessionStatus(Student student, Subject subject, SessionStatus status);

    long countBySessionSubjectAndSessionSectionAndSessionStatus(Subject subject, com.example.demo.entity.Section section, SessionStatus status);

    long countBySessionSectionId(Long sectionId);

    @Query("""
        select a.student.studentId as studentId, count(a.id) as presentCount
        from Attendance a
        where a.session.subject.id = :subjectId
          and a.session.section.id = :sectionId
          and a.session.status = :sessionStatus
          and a.status = :attendanceStatus
        group by a.student.studentId
        """)
    List<Object[]> countPresentByStudent(
        @Param("subjectId") Long subjectId,
        @Param("sectionId") Long sectionId,
        @Param("sessionStatus") SessionStatus sessionStatus,
        @Param("attendanceStatus") AttendanceStatus attendanceStatus
    );

    @Query("""
        select a.session.subject.id as subjectId, count(a.id) as presentCount
        from Attendance a
        where a.student = :student
          and a.session.status = :sessionStatus
          and a.status = :attendanceStatus
        group by a.session.subject.id
        """)
    List<Object[]> countPresentBySubjectForStudent(
        @Param("student") Student student,
        @Param("sessionStatus") SessionStatus sessionStatus,
        @Param("attendanceStatus") AttendanceStatus attendanceStatus
    );
}
