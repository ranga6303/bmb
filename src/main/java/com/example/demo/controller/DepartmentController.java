package com.example.demo.controller;

import com.example.demo.entity.Section;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/departments")
@Transactional(readOnly = true)
public class DepartmentController {

    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    public DepartmentController(SectionRepository sectionRepository, StudentRepository studentRepository, AttendanceRepository attendanceRepository) {
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{name}/report")
    public ResponseEntity<Map<String, Object>> report(@PathVariable String name) {
        List<Section> sections = sectionRepository.findByDepartmentName(name);
        long totalSections = sections.size();
        long totalStudents = sections.stream()
            .mapToLong(section -> studentRepository.countBySectionId(section.getId()))
            .sum();
        long totalAttendance = sections.stream()
            .mapToLong(section -> attendanceRepository.countBySessionSectionId(section.getId()))
            .sum();

        Map<String, Object> report = Map.of(
            "departmentName", name,
            "totalSections", totalSections,
            "totalStudents", totalStudents,
            "totalAttendanceRecords", totalAttendance
        );

        return ResponseEntity.ok(report);
    }
}
