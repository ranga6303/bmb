package com.example.demo.controller;

import com.example.demo.dto.ActiveSessionResponse;
import com.example.demo.dto.DeviceChangeRequestDto;
import com.example.demo.dto.MarkAttendanceRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.StudentOwnAttendanceReport;
import com.example.demo.entity.DeviceChangeRequest;
import com.example.demo.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.DeviceChangeService;
import com.example.demo.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
public class StudentController {
    private final CurrentUserService currentUserService;
    private final SessionService sessionService;
    private final DeviceChangeService deviceChangeService;

    public StudentController(CurrentUserService currentUserService, SessionService sessionService, DeviceChangeService deviceChangeService) {
        this.currentUserService = currentUserService;
        this.sessionService = sessionService;
        this.deviceChangeService = deviceChangeService;
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User user = currentUserService.getCurrentUser();
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        return ResponseEntity.ok(data);
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @GetMapping("/active-session")
    public ResponseEntity<ActiveSessionResponse> activeSession() {
        return ResponseEntity.ok(sessionService.getActiveSessionForStudent(currentUserService.getCurrentUser()));
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @GetMapping("/my-attendance")
    public ResponseEntity<StudentOwnAttendanceReport> myAttendance() {
        return ResponseEntity.ok(sessionService.getOwnAttendanceReport(currentUserService.getCurrentUser()));
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @PostMapping("/attendance")
    public ResponseEntity<MessageResponse> markAttendance(@Valid @RequestBody MarkAttendanceRequest request) {
        sessionService.markAttendance(currentUserService.getCurrentUser(), request);
        return ResponseEntity.ok(new MessageResponse("Attendance marked."));
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @PostMapping("/device-change-request")
    public ResponseEntity<MessageResponse> requestDeviceChange(@Valid @RequestBody DeviceChangeRequestDto dto) {
        return ResponseEntity.ok(deviceChangeService.submitRequest(currentUserService.getCurrentUser(), dto));
    }

    @PreAuthorize("hasAuthority('VIEW_OWN_ATTENDANCE')")
    @GetMapping("/device-change-requests")
    public ResponseEntity<List<Map<String, Object>>> getMyDeviceChangeRequests() {
        List<DeviceChangeRequest> requests = deviceChangeService.getMyRequests(currentUserService.getCurrentUser());
        List<Map<String, Object>> response = requests.stream().map(r -> Map.<String, Object>of(
            "id", r.getId(),
            "oldDeviceId", r.getOldDeviceId(),
            "newDeviceId", r.getNewDeviceId(),
            "reason", r.getReason() != null ? r.getReason() : "",
            "status", r.getStatus().name(),
            "requestedAt", r.getRequestedAt().toString(),
            "adminRemarks", r.getAdminRemarks() != null ? r.getAdminRemarks() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
