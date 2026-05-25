package com.example.demo.controller;

import com.example.demo.dto.SectionAttendanceReport;
import com.example.demo.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sections")
@Transactional(readOnly = true)
public class SectionController {
    private final SessionService sessionService;

    public SectionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PreAuthorize("hasAuthority('VIEW_SECTION_ATTENDANCE')")
    @GetMapping("/{id}/attendance")
    public ResponseEntity<SectionAttendanceReport> attendance(@PathVariable Long id, @RequestParam Long subjectId) {
        return ResponseEntity.ok(sessionService.getSectionSubjectReport(id, subjectId));
    }
}
