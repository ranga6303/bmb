package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.TeacherSectionSubject;
import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.TeacherSectionSubjectRepository;
import com.example.demo.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {
    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    @InjectMocks
    private DepartmentController departmentController;

    @Test
    void hodCannotViewAnotherDepartmentReport() {
        User hodUser = new User();
        hodUser.setId(1L);
        hodUser.setRole(Role.HOD);

        Teacher hodTeacher = new Teacher();
        hodTeacher.setId(10L);
        hodTeacher.setUser(hodUser);

        Section ownSection = new Section();
        ownSection.setId(100L);
        ownSection.setDepartmentName("CSE");

        TeacherSectionSubject assignment = new TeacherSectionSubject();
        assignment.setTeacher(hodTeacher);
        assignment.setSection(ownSection);

        when(currentUserService.getCurrentUser()).thenReturn(hodUser);
        when(teacherRepository.findByUser(hodUser)).thenReturn(Optional.of(hodTeacher));
        when(teacherSectionSubjectRepository.findByTeacher(hodTeacher)).thenReturn(List.of(assignment));

        assertThrows(CustomException.class, () -> departmentController.report("ECE"));
    }
}
