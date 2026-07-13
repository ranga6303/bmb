package com.example.demo.controller;

import com.example.demo.entity.AttendanceStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.SessionStatus;
import com.example.demo.entity.Subject;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/departments")
@Transactional(readOnly = true)
public class DepartmentController {

    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserService currentUserService;
    private final TeacherRepository teacherRepository;
    private final TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    public DepartmentController(
        SectionRepository sectionRepository,
        StudentRepository studentRepository,
        AttendanceRepository attendanceRepository,
        SessionRepository sessionRepository,
        SubjectRepository subjectRepository,
        CurrentUserService currentUserService,
        TeacherRepository teacherRepository,
        TeacherSectionSubjectRepository teacherSectionSubjectRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserService = currentUserService;
        this.teacherRepository = teacherRepository;
        this.teacherSectionSubjectRepository = teacherSectionSubjectRepository;
    }

    @PreAuthorize("hasAuthority('VIEW_DEPARTMENT_ANALYTICS')")
    @GetMapping("/{name}/report")
    public ResponseEntity<Map<String, Object>> report(@PathVariable String name) {
        User actor = currentUserService.getCurrentUser();
        ensureHodCanViewRequestedDepartment(actor, name);

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

    private void ensureHodCanViewRequestedDepartment(User actor, String requestedDepartment) {
        if (actor.getRole() != Role.HOD) {
            return;
        }

        Teacher teacher = teacherRepository.findByUser(actor)
            .orElseThrow(() -> new CustomException("Teacher profile not found"));
        List<TeacherSectionSubject> assignments = teacherSectionSubjectRepository.findByTeacher(teacher);
        if (assignments.isEmpty()) {
            throw new CustomException("HOD has no mapped sections");
        }

        String hodDepartment = assignments.get(0).getSection().getDepartmentName();
        if (hodDepartment == null || requestedDepartment == null
            || !hodDepartment.trim().equalsIgnoreCase(requestedDepartment.trim())) {
            throw new CustomException("You can only view your own department's report.");
        }
    }
}
