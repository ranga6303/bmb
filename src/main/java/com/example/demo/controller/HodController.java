package com.example.demo.controller;

import com.example.demo.dto.AssignClassTeacherRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.AttendanceStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.TeacherSectionSubject;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.TeacherSectionSubjectRepository;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hod")
@Transactional
public class HodController {
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    public HodController(
        TeacherRepository teacherRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository,
        CurrentUserService currentUserService,
        StudentRepository studentRepository,
        AttendanceRepository attendanceRepository,
        SessionRepository sessionRepository,
        TeacherSectionSubjectRepository teacherSectionSubjectRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserService = currentUserService;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.teacherSectionSubjectRepository = teacherSectionSubjectRepository;
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @GetMapping("/teachers")
    public ResponseEntity<List<Map<String, Object>>> getTeachers() {
        List<Map<String, Object>> response = teacherRepository.findAllWithMappings().stream()
            .map(this::toHodTeacherResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('VIEW_DEPARTMENT_ANALYTICS') or hasAuthority('ASSIGN_TEACHER_SECTION')")
    @GetMapping("/department/report")
    public ResponseEntity<Map<String, Object>> departmentReport() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        if (teacher.getMappedSections().isEmpty()) {
            throw new CustomException("HOD has no mapped sections");
        }
        String name = teacher.getMappedSections().iterator().next().getDepartmentName();

        List<Section> sections = sectionRepository.findByDepartmentName(name);
        long totalSections = sections.size();
        long totalStudents = 0;
        List<Map<String, Object>> sectionReports = new ArrayList<>();

        for (Section section : sections) {
            long sectionStudentCount = studentRepository.countBySectionId(section.getId());
            totalStudents += sectionStudentCount;

            List<Map<String, Object>> subjectReports = new ArrayList<>();
            List<Subject> subjects = subjectRepository.findByMappedSectionsContaining(section);
            for (Subject subject : subjects) {
                long approvedSessions = sessionRepository.countBySubjectAndSectionAndStatus(
                    subject,
                    section,
                    SessionStatus.APPROVED
                );
                if (approvedSessions == 0) {
                    continue;
                }

                long totalPresent = attendanceRepository.countPresentByStudent(
                    subject.getId(),
                    section.getId(),
                    SessionStatus.APPROVED,
                    AttendanceStatus.PRESENT
                ).stream()
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .sum();
                long totalPossible = approvedSessions * sectionStudentCount;
                double averageAttendancePercentage = totalPossible == 0
                    ? 0.0
                    : (totalPresent * 100.0) / totalPossible;

                Map<String, Object> subjectReport = new LinkedHashMap<>();
                subjectReport.put("subjectId", subject.getId());
                subjectReport.put("subjectName", subject.getName());
                subjectReport.put("totalApprovedSessions", approvedSessions);
                subjectReport.put("averageAttendancePercentage", averageAttendancePercentage);
                subjectReports.add(subjectReport);
            }

            Map<String, Object> sectionReport = new LinkedHashMap<>();
            sectionReport.put("sectionId", section.getId());
            sectionReport.put("sectionName", section.getName());
            sectionReport.put("totalStudents", sectionStudentCount);
            sectionReport.put("subjects", subjectReports);
            sectionReports.add(sectionReport);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("departmentName", name);
        report.put("totalSections", totalSections);
        report.put("totalStudents", totalStudents);
        report.put("sections", sectionReports);

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @PostMapping("/teachers/{teacherId}/assign-section-subject")
    public ResponseEntity<MessageResponse> assignTeacherSectionSubject(
        @PathVariable String teacherId,
        @RequestParam Long sectionId,
        @RequestParam Long subjectId
    ) {
        Teacher teacher = teacherRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new CustomException("Teacher not found"));
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (teacherSectionSubjectRepository.existsByTeacherAndSectionAndSubject(teacher, section, subject)) {
            throw new CustomException("Assignment already exists.");
        }
        if (!containsSubject(teacher, subject)) {
            throw new CustomException("Teacher is not qualified to teach this subject. Ask admin to assign the capability first.");
        }
        if (!containsSection(subject, section)) {
            throw new CustomException("Subject is not assigned to this section. Ask admin to assign the subject to the section first.");
        }

        TeacherSectionSubject assignment = new TeacherSectionSubject();
        assignment.setTeacher(teacher);
        assignment.setSection(section);
        assignment.setSubject(subject);
        teacherSectionSubjectRepository.save(assignment);

        teacher.getMappedSections().add(section);
        teacher.getMappedSubjects().add(subject);
        teacherRepository.save(teacher);

        return ResponseEntity.ok(new MessageResponse("Teacher assigned to teach " + subject.getName() + " in " + section.getName()));
    }

    @PreAuthorize("hasAuthority('ASSIGN_TEACHER_SECTION')")
    @PostMapping("/teachers/{teacherId}/remove-section-subject")
    public ResponseEntity<MessageResponse> removeTeacherSectionSubject(
        @PathVariable String teacherId,
        @RequestParam Long sectionId,
        @RequestParam Long subjectId
    ) {
        Teacher teacher = teacherRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new CustomException("Teacher not found"));
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (!teacherSectionSubjectRepository.existsByTeacherAndSectionAndSubject(teacher, section, subject)) {
            throw new CustomException("Assignment does not exist.");
        }

        teacherSectionSubjectRepository.deleteByTeacherAndSectionAndSubject(teacher, section, subject);

        return ResponseEntity.ok(new MessageResponse("Assignment removed."));
    }

    @PreAuthorize("hasAuthority('ASSIGN_CLASS_TEACHER')")
    @PostMapping("/sections/{sectionId}/assign-class-teacher")
    public ResponseEntity<MessageResponse> assignClassTeacher(@PathVariable Long sectionId, @Valid @RequestBody AssignClassTeacherRequest request) {
        Teacher teacher = teacherRepository.findByTeacherId(request.getTeacherId())
            .orElseThrow(() -> new CustomException("Teacher not found"));
        if (teacher.getUser() == null || teacher.getUser().getRole() != Role.CLASS_TEACHER) {
            throw new CustomException("Assigned teacher must have CLASS_TEACHER role.");
        }
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        if (teacherRepository.existsBySectionAndRole(section, Role.CLASS_TEACHER)) {
            throw new CustomException("Section already has a class teacher assigned.");
        }
        teacher.getMappedSections().add(section);
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Class teacher assigned to section."));
    }

    private Map<String, Object> toHodTeacherResponse(Teacher teacher) {
        List<Map<String, Object>> assignedSubjects = teacher.getMappedSubjects().stream()
            .map(subject -> Map.<String, Object>of(
                "subjectId", subject.getId(),
                "subjectName", subject.getName()
            ))
            .collect(Collectors.toList());

        List<Map<String, Object>> teachingAssignments = teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .map(assignment -> Map.<String, Object>of(
                "sectionId", assignment.getSection().getId(),
                "sectionName", assignment.getSection().getName(),
                "subjectId", assignment.getSubject().getId(),
                "subjectName", assignment.getSubject().getName()
            ))
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("teacherId", teacher.getTeacherId());
        response.put("name", teacher.getName());
        response.put("email", teacher.getEmail());
        response.put("role", teacher.getUser() != null ? teacher.getUser().getRole().name() : "NOT_REGISTERED");
        response.put("mappedSections", teacher.getMappedSections().stream()
            .map(section -> Map.<String, Object>of(
                "sectionId", section.getId(),
                "sectionName", section.getName()
            ))
            .collect(Collectors.toList()));
        response.put("mappedSubjects", assignedSubjects);
        response.put("assignedSubjects", assignedSubjects);
        response.put("teachingAssignments", teachingAssignments);
        return response;
    }

    private boolean containsSubject(Teacher teacher, Subject subject) {
        return teacher.getMappedSubjects().stream()
            .anyMatch(mappedSubject -> mappedSubject.getId().equals(subject.getId()));
    }

    private boolean containsSection(Subject subject, Section section) {
        return subject.getMappedSections().stream()
            .anyMatch(mappedSection -> mappedSection.getId().equals(section.getId()));
    }
}
