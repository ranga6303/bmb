package com.example.demo.controller;
import com.example.demo.entity.Role;
import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.TeacherSectionSubject;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherSectionSubjectRepository;
import com.example.demo.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher")
@Transactional(readOnly = true)
public class TeacherController {
    private final CurrentUserService currentUserService;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    public TeacherController(CurrentUserService currentUserService, SectionRepository sectionRepository, SubjectRepository subjectRepository, TeacherSectionSubjectRepository teacherSectionSubjectRepository) {
        this.currentUserService = currentUserService;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.teacherSectionSubjectRepository = teacherSectionSubjectRepository;
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION') or hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<String> depts = teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .map(a -> a.getSection().getDepartmentName())
            .distinct()
            .toList();
        return ResponseEntity.ok(depts);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION') or hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/sections")
    public ResponseEntity<List<Map<String, Object>>> sections(@RequestParam(required = false) String department) {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<Map<String, Object>> sections = teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .filter(a -> department == null || department.isBlank() || a.getSection().getDepartmentName().equalsIgnoreCase(department))
            .collect(Collectors.toMap(
                a -> a.getSection().getId(),
                a -> Map.<String, Object>of(
                    "id", a.getSection().getId(),
                    "name", a.getSection().getName(),
                    "departmentName", a.getSection().getDepartmentName()
                ),
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .values().stream()
            .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION')")
    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> subjects() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<Map<String, Object>> subjects = teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .collect(Collectors.toMap(
                a -> a.getSubject().getId(),
                a -> Map.<String, Object>of(
                    "id", a.getSubject().getId(),
                    "name", a.getSubject().getName()
                ),
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .values().stream()
            .collect(Collectors.toList());
        return ResponseEntity.ok(subjects);
    }

    @PreAuthorize("hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/my-class-section")
    public ResponseEntity<Map<String, Object>> myClassSection() {
        if (currentUserService.getCurrentUser().getRole() != Role.CLASS_TEACHER) {
            return ResponseEntity.ok(null);
        }

        Teacher teacher = currentUserService.getCurrentTeacher();
        return teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .map(TeacherSectionSubject::getSection)
            .collect(Collectors.toMap(
                Section::getId,
                section -> section,
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .values().stream()
            .findFirst()
            .map(section -> {
                Map<String, Object> response = new HashMap<>();
                response.put("sectionId", section.getId());
                response.put("sectionName", section.getName());
                response.put("departmentName", section.getDepartmentName());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.ok(null));
    }

    @PreAuthorize("hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/sections/{sectionId}/all-subjects")
    public ResponseEntity<List<Map<String, Object>>> allSectionSubjects(@PathVariable Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));

        List<Map<String, Object>> response = subjectRepository.findByMappedSectionsContaining(section).stream()
            .map(subject -> {
                Map<String, Object> row = new HashMap<>();
                row.put("subjectId", subject.getId());
                row.put("subjectName", subject.getName());
                return row;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/sections/{sectionId}/subjects")
    public ResponseEntity<List<Map<String, Object>>> sectionSubjects(@PathVariable Long sectionId) {
        Teacher teacher = currentUserService.getCurrentTeacher();
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));

        List<Map<String, Object>> response = teacherSectionSubjectRepository.findByTeacher(teacher).stream()
            .filter(assignment -> section.getId().equals(assignment.getSection().getId()))
            .map(TeacherSectionSubject::getSubject)
            .collect(Collectors.toMap(
                Subject::getId,
                subject -> subject,
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .values().stream()
            .map(subject -> {
                Map<String, Object> row = new HashMap<>();
                row.put("subjectId", subject.getId());
                row.put("subjectName", subject.getName());
                return row;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/assignments")
    public ResponseEntity<List<Map<String, Object>>> getAssignments() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<TeacherSectionSubject> assignments = teacherSectionSubjectRepository.findByTeacher(teacher);

        List<Map<String, Object>> response = assignments.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sectionId", a.getSection().getId());
            map.put("sectionName", a.getSection().getName());
            map.put("departmentName", a.getSection().getDepartmentName());
            map.put("subjectId", a.getSubject().getId());
            map.put("subjectName", a.getSubject().getName());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
