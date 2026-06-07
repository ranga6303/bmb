package com.example.demo.service;

import com.example.demo.dto.DeviceChangeRequestDto;
import com.example.demo.dto.DeviceChangeResolveDto;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.DeviceChangeRequest;
import com.example.demo.entity.DeviceChangeStatus;
import com.example.demo.entity.User;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.DeviceChangeRequestRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceChangeService {
    private final DeviceChangeRequestRepository deviceChangeRequestRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final UserSessionRepository userSessionRepository;

    public DeviceChangeService(
        DeviceChangeRequestRepository deviceChangeRequestRepository,
        UserRepository userRepository,
        StudentRepository studentRepository,
        UserSessionRepository userSessionRepository
    ) {
        this.deviceChangeRequestRepository = deviceChangeRequestRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public MessageResponse submitRequest(User actor, DeviceChangeRequestDto dto) {
        String newDeviceId = dto.getNewDeviceId() != null ? dto.getNewDeviceId().trim() : null;
        if (newDeviceId == null || newDeviceId.isEmpty()) {
            throw new CustomException("New device ID is required.");
        }

        String currentDeviceId = actor.getRegisteredDeviceId() != null ? actor.getRegisteredDeviceId().trim() : null;

        if (deviceChangeRequestRepository.existsByUserAndStatus(actor, DeviceChangeStatus.PENDING)) {
            throw new CustomException("You already have a pending device change request.");
        }

        DeviceChangeRequest request = new DeviceChangeRequest();
        request.setUser(actor);
        request.setOldDeviceId(currentDeviceId);
        request.setNewDeviceId(newDeviceId);
        request.setReason(dto.getReason());
        request.setStatus(DeviceChangeStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        deviceChangeRequestRepository.save(request);

        return new MessageResponse("Device change request submitted. Awaiting admin approval.");
    }

    @Transactional(readOnly = true)
    public List<DeviceChangeRequest> getPendingRequests() {
        return deviceChangeRequestRepository.findByStatus(DeviceChangeStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<DeviceChangeRequest> getMyRequests(User actor) {
        return deviceChangeRequestRepository.findByUser(actor);
    }

    @Transactional
    public MessageResponse approveRequest(Long requestId, User admin, DeviceChangeResolveDto dto) {
        DeviceChangeRequest request = deviceChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new CustomException("Device change request not found"));

        if (request.getStatus() != DeviceChangeStatus.PENDING) {
            throw new CustomException("Only PENDING requests can be approved.");
        }
        User user = request.getUser();

        // Reset device binding
        user.setRegisteredDeviceId(request.getNewDeviceId());
        userRepository.save(user);

        // Clear public key so student must re-register with new device
        studentRepository.findByUser(user).ifPresent(student -> {
            student.setPublicKey(null);
            studentRepository.save(student);
        });

        // Revoke all sessions
        userSessionRepository.findByUserAndRevokedFalse(user).forEach(session -> {
            session.setRevoked(true);
            userSessionRepository.save(session);
        });

        request.setStatus(DeviceChangeStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin);
        request.setAdminRemarks(dto != null ? dto.getAdminRemarks() : null);
        deviceChangeRequestRepository.save(request);

        return new MessageResponse("Device change request approved. User's device binding has been updated.");
    }

    @Transactional
    public MessageResponse rejectRequest(Long requestId, User admin, DeviceChangeResolveDto dto) {
        DeviceChangeRequest request = deviceChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new CustomException("Device change request not found"));

        if (request.getStatus() != DeviceChangeStatus.PENDING) {
            throw new CustomException("Only PENDING requests can be rejected.");
        }

        request.setStatus(DeviceChangeStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin);
        request.setAdminRemarks(dto != null ? dto.getAdminRemarks() : null);
        deviceChangeRequestRepository.save(request);

        return new MessageResponse("Device change request rejected.");
    }
}
