package com.example.demo.service;

import com.example.demo.entity.Session;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.AttendanceBufferRepository;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.TeacherSectionSubjectRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceOwnershipTest {
    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AttendanceBufferRepository attendanceBufferRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private DeviceVerificationService deviceVerificationService;

    @Mock
    private TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    @Mock
    private DeviceChangeService deviceChangeService;

    private SessionService sessionService;
    private User actor;
    private Teacher owningTeacher;
    private Teacher otherTeacher;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
            teacherRepository,
            subjectRepository,
            sectionRepository,
            sessionRepository,
            attendanceBufferRepository,
            attendanceRepository,
            studentRepository,
            auditLogRepository,
            userRepository,
            roomService,
            deviceVerificationService,
            teacherSectionSubjectRepository,
            deviceChangeService
        );

        actor = new User();
        actor.setId(1L);

        owningTeacher = new Teacher();
        owningTeacher.setId(10L);

        otherTeacher = new Teacher();
        otherTeacher.setId(20L);
    }

    @Test
    void lockSessionRejectsNonOwningTeacher() {
        Session session = sessionWithOwner(SessionStatus.ACTIVE);
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(teacherRepository.findByUser(actor)).thenReturn(Optional.of(otherTeacher));

        assertThrows(CustomException.class, () -> sessionService.lockSession(100L, actor));

        verify(sessionRepository, never()).save(session);
    }

    @Test
    void approveSessionRejectsNonOwningTeacher() {
        Session session = sessionWithOwner(SessionStatus.LOCKED);
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(teacherRepository.findByUser(actor)).thenReturn(Optional.of(otherTeacher));

        assertThrows(CustomException.class, () -> sessionService.approveSession(100L, actor));

        verify(sessionRepository, never()).save(session);
    }

    @Test
    void cancelSessionRejectsNonOwningTeacher() {
        Session session = sessionWithOwner(SessionStatus.LOCKED);
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(teacherRepository.findByUser(actor)).thenReturn(Optional.of(otherTeacher));

        assertThrows(CustomException.class, () -> sessionService.cancelSession(100L, actor));

        verify(sessionRepository, never()).save(session);
    }

    private Session sessionWithOwner(SessionStatus status) {
        Session session = new Session();
        session.setId(100L);
        session.setTeacher(owningTeacher);
        session.setStatus(status);
        return session;
    }
}
