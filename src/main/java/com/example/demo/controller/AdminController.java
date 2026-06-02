package com.example.demo.controller;

import com.example.demo.dto.AssignRoleRequest;
import com.example.demo.dto.DeviceChangeResolveDto;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.DeviceChangeRequest;
import com.example.demo.entity.Section;
import com.example.demo.entity.Subject;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.DeviceChangeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@Transactional
public class AdminController {
    private final AuthService authService;
    private final DeviceChangeService deviceChangeService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    public AdminController(
        AuthService authService,
        DeviceChangeService deviceChangeService,
        CurrentUserService currentUserService,
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository
    ) {
        this.authService = authService;
        this.deviceChangeService = deviceChangeService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/users/{userId}/role")
    public ResponseEntity<MessageResponse> assignRole(@PathVariable Long userId, @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(authService.assignRole(userId, request.getRole()));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/reset-device/{userId}")
    public ResponseEntity<MessageResponse> resetDevice(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.resetDeviceBinding(userId));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/teachers")
    public ResponseEntity<List<Map<String, Object>>> getTeachers() {
        List<Map<String, Object>> response = teacherRepository.findAllWithMappings().stream()
            .map(this::toAdminTeacherResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/sections")
    public ResponseEntity<List<Map<String, Object>>> getSections() {
        List<Map<String, Object>> response = sectionRepository.findAll().stream()
            .map(this::toAdminSectionResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> getSubjects() {
        List<Map<String, Object>> response = subjectRepository.findAll().stream()
            .map(this::toAdminSubjectResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/teachers/{teacherId}/assign-subject")
    public ResponseEntity<MessageResponse> assignSubjectToTeacher(@PathVariable String teacherId, @RequestParam Long subjectId) {
        Teacher teacher = teacherRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new CustomException("Teacher not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (containsSubject(teacher, subject)) {
            throw new CustomException("Subject is already assigned to this teacher.");
        }

        teacher.getMappedSubjects().add(subject);
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Subject assigned to teacher."));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/teachers/{teacherId}/remove-subject")
    public ResponseEntity<MessageResponse> removeSubjectFromTeacher(@PathVariable String teacherId, @RequestParam Long subjectId) {
        Teacher teacher = teacherRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new CustomException("Teacher not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (!containsSubject(teacher, subject)) {
            throw new CustomException("Subject is not assigned to this teacher.");
        }

        teacher.getMappedSubjects().removeIf(mappedSubject -> mappedSubject.getId().equals(subject.getId()));
        teacherRepository.save(teacher);
        return ResponseEntity.ok(new MessageResponse("Subject removed from teacher."));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/sections/{sectionId}/assign-subject")
    public ResponseEntity<MessageResponse> assignSubjectToSection(@PathVariable Long sectionId, @RequestParam Long subjectId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (subject.getMappedSections().contains(section)) {
            throw new CustomException("Subject is already assigned to this section.");
        }

        subject.getMappedSections().add(section);
        subjectRepository.save(subject);
        return ResponseEntity.ok(new MessageResponse("Subject assigned to section."));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/sections/{sectionId}/remove-subject")
    public ResponseEntity<MessageResponse> removeSubjectFromSection(@PathVariable Long sectionId, @RequestParam Long subjectId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException("Section not found"));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new CustomException("Subject not found"));

        if (!subject.getMappedSections().contains(section)) {
            throw new CustomException("Subject is not assigned to this section.");
        }

        subject.getMappedSections().remove(section);
        subjectRepository.save(subject);
        return ResponseEntity.ok(new MessageResponse("Subject removed from section."));
    }

    // DISABLED: Android ID based blocking system commented out.
    // Reason: Cannot distinguish between legitimate reinstall and proxy attack
    // using same Android ID. Physical teacher verification is the intended
    // solution for device reset scenarios. Re-enable when proper
    // verification flow is implemented.
    /*
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/blocked-students")
    public ResponseEntity<List<Map<String, Object>>> getBlockedStudents() {
        List<User> blockedUsers = userRepository.findByBlocked(true);
        List<Map<String, Object>> response = blockedUsers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getId());
            map.put("username", u.getUsername());
            map.put("blockReason", u.getBlockReason());
            map.put("blockedAt", u.getBlockedAt() != null ? u.getBlockedAt().toString() : null);
            map.put("currentAndroidId", u.getCurrentAndroidId());
            map.put("previousAndroidId", u.getPreviousAndroidId());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/unblock/{userId}")
    public ResponseEntity<MessageResponse> unblockStudent(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));
        user.setBlocked(false);
        user.setBlockReason(null);
        user.setBlockedAt(null);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User unblocked successfully."));
    }
    */

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/device-change-requests")
    public ResponseEntity<List<Map<String, Object>>> getPendingDeviceChanges() {
        List<DeviceChangeRequest> requests = deviceChangeService.getPendingRequests();
        List<Map<String, Object>> response = requests.stream().map(r -> Map.<String, Object>of(
            "id", r.getId(),
            "userId", r.getUser().getId(),
            "username", r.getUser().getUsername(),
            "oldDeviceId", r.getOldDeviceId(),
            "newDeviceId", r.getNewDeviceId(),
            "reason", r.getReason() != null ? r.getReason() : "",
            "requestedAt", r.getRequestedAt().toString()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/device-change-requests/{id}/approve")
    public ResponseEntity<MessageResponse> approveDeviceChange(@PathVariable Long id, @Valid @RequestBody(required = false) DeviceChangeResolveDto dto) {
        return ResponseEntity.ok(deviceChangeService.approveRequest(id, currentUserService.getCurrentUser(), dto));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping("/device-change-requests/{id}/reject")
    public ResponseEntity<MessageResponse> rejectDeviceChange(@PathVariable Long id, @Valid @RequestBody(required = false) DeviceChangeResolveDto dto) {
        return ResponseEntity.ok(deviceChangeService.rejectRequest(id, currentUserService.getCurrentUser(), dto));
    }

    private Map<String, Object> toAdminTeacherResponse(Teacher teacher) {
        User user = teacher.getUser();
        Map<String, Object> response = new HashMap<>();
        response.put("teacherId", teacher.getTeacherId());
        response.put("name", teacher.getName());
        response.put("email", teacher.getEmail());
        response.put("userId", user != null ? user.getId() : null);
        response.put("role", user != null ? user.getRole().name() : "NOT_REGISTERED");
        response.put("registeredDeviceId", user != null ? user.getRegisteredDeviceId() : null);
        response.put("assignedSubjects", teacher.getMappedSubjects().stream()
            .map(this::toAdminSubjectResponse)
            .collect(Collectors.toList()));
        return response;
    }

    private Map<String, Object> toAdminSectionResponse(Section section) {
        Map<String, Object> response = new HashMap<>();
        response.put("sectionId", section.getId());
        response.put("sectionName", section.getName());
        response.put("departmentName", section.getDepartmentName());
        response.put("assignedSubjects", subjectRepository.findByMappedSectionsContaining(section).stream()
            .map(this::toAdminSubjectResponse)
            .collect(Collectors.toList()));
        return response;
    }

    private Map<String, Object> toAdminSubjectResponse(Subject subject) {
        Map<String, Object> response = new HashMap<>();
        response.put("subjectId", subject.getId());
        response.put("subjectName", subject.getName());
        return response;
    }

    private boolean containsSubject(Teacher teacher, Subject subject) {
        return teacher.getMappedSubjects().stream()
            .anyMatch(mappedSubject -> mappedSubject.getId().equals(subject.getId()));
    }
}
