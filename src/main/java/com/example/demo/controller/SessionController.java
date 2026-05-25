package com.example.demo.controller;

import com.example.demo.dto.CreateSessionRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.AttendanceBuffer;
import com.example.demo.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.SessionService;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sessions")
public class SessionController {
    private final SessionService sessionService;
    private final CurrentUserService currentUserService;

    public SessionController(SessionService sessionService, CurrentUserService currentUserService) {
        this.sessionService = sessionService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @PostMapping
    public ResponseEntity<MessageResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        User actor = currentUserService.getCurrentUser();
        Long sessionId = sessionService.createSession(actor, request.getSubjectId(), request.getSectionId(), request.getRoomNumber(), request.getBeaconUuid());
        return ResponseEntity.ok(new MessageResponse("Session created successfully. ID:" + sessionId));
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSession() {
        User actor = currentUserService.getCurrentUser();
        return sessionService.getActiveSessionForTeacher(actor)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PreAuthorize("hasAuthority('LOCK_SESSION')")
    @PostMapping("/{id}/lock")
    public ResponseEntity<MessageResponse> lock(@PathVariable Long id) {
        sessionService.lockSession(id, currentUserService.getCurrentUser());
        return ResponseEntity.ok(new MessageResponse("Session locked."));
    }

    @PreAuthorize("hasAuthority('APPROVE_SESSION')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<MessageResponse> approve(@PathVariable Long id) {
        sessionService.approveSession(id, currentUserService.getCurrentUser());
        return ResponseEntity.ok(new MessageResponse("Session approved."));
    }

    @PreAuthorize("hasAuthority('CANCEL_SESSION')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse> cancel(@PathVariable Long id) {
        sessionService.cancelSession(id, currentUserService.getCurrentUser());
        return ResponseEntity.ok(new MessageResponse("Session cancelled."));
    }

    @PreAuthorize("hasAuthority('MANUAL_MARK_ATTENDANCE')")
    @PostMapping("/{id}/manual")
    public ResponseEntity<MessageResponse> manual(@PathVariable Long id, @RequestParam String studentId) {
        sessionService.manualMark(id, studentId, currentUserService.getCurrentUser());
        return ResponseEntity.ok(new MessageResponse("Manual attendance marked."));
    }

    @PreAuthorize("hasAuthority('CREATE_SESSION')")
    @GetMapping("/{id}/buffer")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getBuffer(@PathVariable Long id) {
        List<AttendanceBuffer> buffer = sessionService.getSessionBuffer(id, currentUserService.getCurrentUser());
        List<Map<String, Object>> response = buffer.stream().map(b -> Map.<String, Object>of(
            "studentId", b.getStudent().getStudentId(),
            "studentName", b.getStudent().getName(),
            "markType", b.getMarkType().name(),
            "markedAt", b.getMarkedAt().toString()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
