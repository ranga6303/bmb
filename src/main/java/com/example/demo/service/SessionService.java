package com.example.demo.service;

import com.example.demo.dto.ActiveSessionResponse;
import com.example.demo.dto.MarkAttendanceRequest;
import com.example.demo.dto.DeviceChangeRequestDto;
import com.example.demo.dto.SectionAttendanceReport;
import com.example.demo.dto.StudentAttendanceSummary;
import com.example.demo.dto.StudentOwnAttendanceReport;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.AttendanceBuffer;
import com.example.demo.entity.AttendanceStatus;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.MarkType;
import com.example.demo.entity.Section;
import com.example.demo.entity.Session;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Student;
import com.example.demo.entity.Subject;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SessionService {
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceBufferRepository attendanceBufferRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final DeviceVerificationService deviceVerificationService;
    private final TeacherSectionSubjectRepository teacherSectionSubjectRepository;
    private final DeviceChangeService deviceChangeService;

    public SessionService(
        TeacherRepository teacherRepository,
        SubjectRepository subjectRepository,
        SectionRepository sectionRepository,
        SessionRepository sessionRepository,
        AttendanceBufferRepository attendanceBufferRepository,
        AttendanceRepository attendanceRepository,
        StudentRepository studentRepository,
        AuditLogRepository auditLogRepository,
        UserRepository userRepository,
        RoomService roomService,
        DeviceVerificationService deviceVerificationService,
        TeacherSectionSubjectRepository teacherSectionSubjectRepository,
        DeviceChangeService deviceChangeService
    ) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceBufferRepository = attendanceBufferRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.deviceVerificationService = deviceVerificationService;
        this.teacherSectionSubjectRepository = teacherSectionSubjectRepository;
        this.deviceChangeService = deviceChangeService;
    }

    @Transactional
    public Long createSession(User actor, Long subjectId, Long sectionId, String roomNumber) {
        return createSession(actor, subjectId, sectionId, roomNumber, null);
    }

    @Transactional
    public Long createSession(User actor, Long subjectId, Long sectionId, String roomNumber, String beaconUuid) {
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));

        if (!teacherSectionSubjectRepository.existsByTeacherAndSectionAndSubject(teacher, section, subject)) {
            throw new CustomException("You are not assigned to teach this subject in this section. Contact HOD.");
        }

        // Time-based validations
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        // if (day == DayOfWeek.SUNDAY) {
        //     throw new CustomException("Sessions cannot be created on Sundays.");
        // }
        // LocalTime currentTime = now.toLocalTime();
        // if (currentTime.isBefore(LocalTime.of(7, 0)) || currentTime.isAfter(LocalTime.of(23, 0))) {
        //     throw new CustomException("Sessions can only be created between 07:00 and 23:00.");
        // }

        // Limit one session per subject per section per day
        // if (sessionRepository.existsBySubjectAndSectionAndSessionDate(subject, section, LocalDate.now())) {
        //     throw new CustomException("A session for this subject in this section has already been created today.");
        // }

        if (sessionRepository.existsByTeacherAndStatus(teacher, SessionStatus.ACTIVE)
            || sessionRepository.existsByTeacherAndStatus(teacher, SessionStatus.LOCKED)) {
            throw new CustomException("Teacher already has an active or locked session");
        }

        if (sessionRepository.existsBySectionAndStatus(section, SessionStatus.ACTIVE)
            || sessionRepository.existsBySectionAndStatus(section, SessionStatus.LOCKED)) {
            throw new CustomException("Section already has an active or locked session");
        }

        Session session = new Session();
        session.setTeacher(teacher);
        session.setSubject(subject);
        session.setSection(section);
        session.setRoom(roomService.getRoomForSession(roomNumber, beaconUuid));
        String code;
        SecureRandom random = new SecureRandom();
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (sessionRepository.existsBySessionCode(code));
        session.setSessionCode(code);
        session.setSessionDate(LocalDate.now());
        session.setStartTime(now);
        session.setExpiryTime(now.plusMinutes(60));
        session.setStatus(SessionStatus.ACTIVE);
        Session saved = sessionRepository.save(session);
        persistAudit("SESSION_CREATED", actor, "Session", saved.getId(), "Teacher username: " + actor.getUsername());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getActiveSessionForTeacher(User actor) {
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        return sessionRepository.findByTeacherAndStatusIn(
            teacher,
            List.of(SessionStatus.ACTIVE, SessionStatus.LOCKED)
        ).map(session -> Map.of(
            "sessionId", session.getId(),
            "subjectName", session.getSubject().getName(),
            "sectionName", session.getSection().getName(),
            "status", session.getStatus().name()
        ));
    }

    @Transactional(readOnly = true)
    public ActiveSessionResponse getActiveSessionForStudent(User actor) {
        Student student = studentRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Student profile not found"));

        Session session = sessionRepository.findBySectionAndStatus(student.getSection(), SessionStatus.ACTIVE)
            .orElseThrow(() -> new CustomException("No active session found."));

        if (session.getExpiryTime() != null && session.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new CustomException("Session has expired.");
        }

        ActiveSessionResponse response = new ActiveSessionResponse();
        response.setSessionId(session.getId());
        response.setSessionCode(session.getSessionCode());
        response.setSubjectName(session.getSubject().getName());
        response.setTeacherName(session.getTeacher().getName());
        response.setRoomNumber(session.getRoom().getRoomNumber());
        response.setBeaconUuid(session.getRoom().getBeaconUuid());
        response.setExpiryTime(session.getExpiryTime());
        return response;
    }

    @Transactional
    public void markAttendance(User actor, MarkAttendanceRequest request) {
        Session session = sessionRepository.findBySessionCodeAndStatus(request.getSessionCode(), SessionStatus.ACTIVE)
            .orElseThrow(() -> new CustomException("Active session not found for provided code."));

        if (session.getExpiryTime() != null && session.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new CustomException("Session has expired.");
        }

        Student student = studentRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Student profile not found"));

        if (!student.getSection().getId().equals(session.getSection().getId())) {
            throw new CustomException("Student does not belong to this session section.");
        }

        if (!request.getBeaconUuid().equals(session.getRoom().getBeaconUuid())) {
            throw new CustomException("Beacon UUID does not match the classroom. Are you physically present?");
        }

        handleDeviceBinding(actor, student, request);

        if (attendanceBufferRepository.existsBySessionAndStudent(session, student)) {
            throw new CustomException("Attendance already marked for this session.");
        }

        AttendanceBuffer attendanceBuffer = new AttendanceBuffer();
        attendanceBuffer.setSession(session);
        attendanceBuffer.setStudent(student);
        attendanceBuffer.setMarkType(MarkType.AUTO);
        attendanceBufferRepository.save(attendanceBuffer);
    }

    @Transactional
    public void lockSession(Long sessionId, User actor) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new CustomException("Session not found"));
        validateNotTerminal(session);
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException("Only owning teacher can lock this session.");
        }
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new CustomException("Only ACTIVE sessions can be locked.");
        }
        session.setStatus(SessionStatus.LOCKED);
        session.setLockedAt(LocalDateTime.now());
        sessionRepository.save(session);
        persistAudit("SESSION_LOCKED", actor, "Session", session.getId(), "Session locked");
    }

    @Transactional
    public void approveSession(Long sessionId, User actor) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new CustomException("Session not found"));
        validateNotTerminal(session);
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        if (session.getStatus() != SessionStatus.LOCKED) {
            throw new CustomException("Only LOCKED sessions can be approved.");
        }
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException("Only owning teacher can approve this session.");
        }

        List<AttendanceBuffer> bufferList = attendanceBufferRepository.findBySession(session);
        Map<Long, AttendanceBuffer> marked = new HashMap<>();
        for (AttendanceBuffer item : bufferList) {
            marked.put(item.getStudent().getId(), item);
        }

        List<Student> students = studentRepository.findBySectionId(session.getSection().getId());
        for (Student student : students) {
            Attendance attendance = new Attendance();
            attendance.setSession(session);
            attendance.setStudent(student);
            attendance.setStatus(marked.containsKey(student.getId()) ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
            attendanceRepository.save(attendance);
        }

        session.setStatus(SessionStatus.APPROVED);
        session.setApprovedAt(LocalDateTime.now());
        sessionRepository.save(session);
        attendanceBufferRepository.deleteBySession(session);
        persistAudit("SESSION_APPROVED", actor, "Session", session.getId(), "Session approved");
    }

    @Transactional
    public void cancelSession(Long sessionId, User actor) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new CustomException("Session not found"));
        validateNotTerminal(session);
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException("Only owning teacher can cancel this session.");
        }
        if (session.getStatus() != SessionStatus.LOCKED) {
            throw new CustomException("Only LOCKED sessions can be cancelled.");
        }
        attendanceBufferRepository.deleteBySession(session);
        session.setStatus(SessionStatus.CANCELLED);
        session.setCancelledAt(LocalDateTime.now());
        sessionRepository.save(session);
        persistAudit("SESSION_CANCELLED", actor, "Session", session.getId(), "Session cancelled");
    }

    @Transactional
    public void manualMark(Long sessionId, String studentId, User actor) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new CustomException("Session not found"));
        validateNotTerminal(session);
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new CustomException("Session must be ACTIVE for manual marking.");
        }
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException("Only owning teacher can manually mark attendance.");
        }

        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new CustomException("Student not found"));
        if (!student.getSection().getId().equals(session.getSection().getId())) {
            throw new CustomException("Student does not belong to this section.");
        }

        if (attendanceBufferRepository.existsBySessionAndStudent(session, student)) {
            throw new CustomException("Student attendance already marked in this session.");
        }

        AttendanceBuffer buffer = new AttendanceBuffer();
        buffer.setSession(session);
        buffer.setStudent(student);
        buffer.setMarkType(MarkType.MANUAL);
        attendanceBufferRepository.save(buffer);
        persistAudit("MANUAL_ATTENDANCE", actor, "Session", session.getId(), "StudentId: " + student.getStudentId());
    }

    @Transactional(readOnly = true)
    public SectionAttendanceReport getSectionSubjectReport(Long sectionId, Long subjectId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        long total = sessionRepository.countBySubjectAndSectionAndStatus(subject, section, SessionStatus.APPROVED);

        List<Object[]> rows = attendanceRepository.countPresentByStudent(
            subjectId,
            sectionId,
            SessionStatus.APPROVED,
            AttendanceStatus.PRESENT
        );
        Map<String, Long> presentByStudentId = new HashMap<>();
        for (Object[] row : rows) {
            presentByStudentId.put((String) row[0], (Long) row[1]);
        }

        List<Student> students = studentRepository.findBySectionId(sectionId);
        SectionAttendanceReport report = new SectionAttendanceReport();
        report.setSectionId(sectionId);
        report.setSubjectId(subjectId);

        for (Student student : students) {
            long attended = presentByStudentId.getOrDefault(student.getStudentId(), 0L);
            StudentAttendanceSummary summary = new StudentAttendanceSummary();
            summary.setStudentId(student.getStudentId());
            summary.setStudentName(student.getName());
            summary.setAttended(attended);
            summary.setTotal(total);
            summary.setPercentage(total == 0 ? 0.0 : (attended * 100.0) / total);
            report.getStudents().add(summary);
        }

        return report;
    }

    @Transactional(readOnly = true)
    public StudentOwnAttendanceReport getOwnAttendanceReport(User user) {
        Student student = studentRepository.findByUser(user)
            .orElseThrow(() -> new CustomException("Student profile not found"));

        Section section = student.getSection();
        List<Subject> subjects = subjectRepository.findAll();

        // Count present per subject for this student
        List<Object[]> rows = attendanceRepository.countPresentBySubjectForStudent(
            student, SessionStatus.APPROVED, AttendanceStatus.PRESENT
        );
        Map<Long, Long> presentBySubjectId = new HashMap<>();
        for (Object[] row : rows) {
            presentBySubjectId.put((Long) row[0], (Long) row[1]);
        }

        StudentOwnAttendanceReport report = new StudentOwnAttendanceReport();
        report.setStudentId(student.getStudentId());
        report.setStudentName(student.getName());
        report.setSectionName(section.getName());

        for (Subject subject : subjects) {
            long total = sessionRepository.countBySubjectAndSectionAndStatus(subject, section, SessionStatus.APPROVED);
            if (total == 0) continue; // skip subjects with no sessions for this section
            long attended = presentBySubjectId.getOrDefault(subject.getId(), 0L);
            StudentOwnAttendanceReport.SubjectAttendance sa = new StudentOwnAttendanceReport.SubjectAttendance();
            sa.setSubjectId(subject.getId());
            sa.setSubjectName(subject.getName());
            sa.setAttended(attended);
            sa.setTotal(total);
            sa.setPercentage((attended * 100.0) / total);
            report.getSubjects().add(sa);
        }

        return report;
    }

    @Transactional
    public int expireActiveSessions() {
        List<Session> sessions = sessionRepository.findByStatusAndExpiryTimeBefore(SessionStatus.ACTIVE, LocalDateTime.now());
        for (Session session : sessions) {
            session.setStatus(SessionStatus.LOCKED);
            session.setLockedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
        return sessions.size();
    }

    @Transactional
    public int autoCancelLockedSessions() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Session> sessions = sessionRepository.findByStatusAndLockedAtBefore(SessionStatus.LOCKED, oneHourAgo);
        for (Session session : sessions) {
            attendanceBufferRepository.deleteBySession(session);
            session.setStatus(SessionStatus.CANCELLED);
            session.setCancelledAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
        return sessions.size();
    }

    @Transactional(readOnly = true)
    public List<AttendanceBuffer> getSessionBuffer(Long sessionId, User actor) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new CustomException("Session not found"));
        validateNotTerminal(session);
        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException("Only owning teacher can view session buffer.");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new CustomException("Cannot view buffer of a cancelled session.");
        }
        return attendanceBufferRepository.findBySession(session);
    }

    private void validateNotTerminal(Session session) {
        if (session.getStatus() == SessionStatus.APPROVED) {
            throw new CustomException("Cannot perform operations on an APPROVED session.");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new CustomException("Cannot perform operations on a CANCELLED session.");
        }
    }

    private void persistAudit(String action, User actor, String entity, Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActorUser(actor);
        log.setTargetEntity(entity);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private void handleDeviceBinding(User actor, Student student, MarkAttendanceRequest request) {
        String deviceId = request.getDeviceId();
        boolean deviceAlreadyBound = actor.getRegisteredDeviceId() != null;

        if (!deviceAlreadyBound) {
            userRepository.findByRegisteredDeviceId(deviceId).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(actor.getId())) {
                    throw new CustomException("This device is already registered to another student account.");
                }
            });
            actor.setRegisteredDeviceId(deviceId);
        } else if (!actor.getRegisteredDeviceId().equals(deviceId)) {
            throw new CustomException("Device mismatch. Contact admin to reset device binding.");
        }

        if (hasText(request.getPublicKey())) {
            studentRepository.findByPublicKey(request.getPublicKey()).ifPresent(existingStudent -> {
                if (!existingStudent.getId().equals(student.getId())) {
                    throw new CustomException("This device is already registered to another student. Please use your own device.");
                }
            });
            validateSignedPayload(request);
            deviceVerificationService.registerPublicKey(student, request.getPublicKey());
            deviceVerificationService.verifySignatureWithKey(request.getPublicKey(), request);
            studentRepository.save(student);
        } else if (student.getPublicKey() != null) {
            validateSignedPayload(request);
            deviceVerificationService.verifySignature(student, request);
        }

        userRepository.save(actor);
    }

    private void validateSignedPayload(MarkAttendanceRequest request) {
        if (!hasText(request.getSignedPayload()) || !hasText(request.getDeviceSignature())) {
            throw new CustomException("Signed payload and device signature are required for device verification.");
        }

        String[] parts = request.getSignedPayload().split(":", -1);
        if (parts.length != 4
            || !parts[0].equals(request.getSessionCode())
            || !parts[1].equals(request.getBeaconUuid())
            || !parts[2].equals(request.getDeviceId())
            || !hasText(parts[3])) {
            throw new CustomException("Invalid signed payload format.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    private void ensureDeviceChangeRequestExists(User actor, MarkAttendanceRequest request, String reason) {
        DeviceChangeRequestDto dto = new DeviceChangeRequestDto();
        dto.setNewDeviceId(request.getDeviceId());
        dto.setReason(reason);

        try {
            deviceChangeService.submitRequest(actor, dto);
        } catch (CustomException ex) {
            if (!"You already have a pending device change request.".equals(ex.getMessage())) {
                throw ex;
            }
        }
    }
}

