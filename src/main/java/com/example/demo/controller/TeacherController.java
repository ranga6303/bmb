package com.example.demo.controller;
import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.TeacherSectionSubject;
import com.example.demo.repository.TeacherSectionSubjectRepository;
import com.example.demo.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher")
@Transactional(readOnly = true)
public class TeacherController {
    private final CurrentUserService currentUserService;
    private final TeacherSectionSubjectRepository teacherSectionSubjectRepository;

    public TeacherController(CurrentUserService currentUserService, TeacherSectionSubjectRepository teacherSectionSubjectRepository) {
        this.currentUserService = currentUserService;
        this.teacherSectionSubjectRepository = teacherSectionSubjectRepository;
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION') or hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<String> depts = teacher.getMappedSections().stream()
            .map(Section::getDepartmentName)
            .distinct()
            .toList();
        return ResponseEntity.ok(depts);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION') or hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/sections")
    public ResponseEntity<List<Section>> sections(@RequestParam(required = false) String department) {
        Teacher teacher = currentUserService.getCurrentTeacher();
        Set<Section> mapped = teacher.getMappedSections();
        List<Section> sections = mapped.stream()
            .filter(s -> department == null || department.isBlank() || s.getDepartmentName().equalsIgnoreCase(department))
            .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION') or hasAuthority('ASSIGN_TEACHER_SECTION')")
    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> subjects() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        return ResponseEntity.ok(teacher.getMappedSubjects().stream().toList());
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
