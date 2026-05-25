package com.example.demo.controller;
import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher")
@Transactional(readOnly = true)
public class TeacherController {
    private final CurrentUserService currentUserService;

    public TeacherController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        List<String> depts = teacher.getMappedSections().stream()
            .map(Section::getDepartmentName)
            .distinct()
            .toList();
        return ResponseEntity.ok(depts);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/sections")
    public ResponseEntity<List<Section>> sections(@RequestParam(required = false) String department) {
        Teacher teacher = currentUserService.getCurrentTeacher();
        Set<Section> mapped = teacher.getMappedSections();
        List<Section> sections = mapped.stream()
            .filter(s -> department == null || department.isBlank() || s.getDepartmentName().equalsIgnoreCase(department))
            .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> subjects() {
        Teacher teacher = currentUserService.getCurrentTeacher();
        return ResponseEntity.ok(teacher.getMappedSubjects().stream().toList());
    }
}
