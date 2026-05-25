package com.example.demo.repository;

import com.example.demo.entity.DeviceChangeRequest;
import com.example.demo.entity.DeviceChangeStatus;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceChangeRequestRepository extends JpaRepository<DeviceChangeRequest, Long> {
    boolean existsByUserAndStatus(User user, DeviceChangeStatus status);

    List<DeviceChangeRequest> findByStatus(DeviceChangeStatus status);

    List<DeviceChangeRequest> findByUser(User user);
}
