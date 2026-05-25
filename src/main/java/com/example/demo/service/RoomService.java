package com.example.demo.service;

import com.example.demo.entity.Room;
import com.example.demo.entity.SessionStatus;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final SessionRepository sessionRepository;

    public RoomService(RoomRepository roomRepository, SessionRepository sessionRepository) {
        this.roomRepository = roomRepository;
        this.sessionRepository = sessionRepository;
    }

    public Room getRoomForSession(String roomNumber) {
        return getRoomForSession(roomNumber, null);
    }

    public Room getRoomForSession(String roomNumber, String beaconUuid) {
        Room room;
        if (roomNumber != null && !roomNumber.isBlank()) {
            room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new CustomException("Room not found"));
        } else if (beaconUuid != null && !beaconUuid.isBlank()) {
            room = roomRepository.findByBeaconUuid(beaconUuid)
                .orElseThrow(() -> new CustomException("Room not found by beacon UUID"));
        } else {
            throw new CustomException("Either roomNumber or beaconUuid is required");
        }

        validateRoom(room);

        // Check room not occupied by another active/locked session
        if (sessionRepository.existsByRoomAndStatus(room, SessionStatus.ACTIVE)
            || sessionRepository.existsByRoomAndStatus(room, SessionStatus.LOCKED)) {
            throw new CustomException("Room is already occupied by an active session");
        }

        return room;
    }

    private void validateRoom(Room room) {
        if (room.getBeaconUuid() == null || room.getBeaconUuid().isBlank()) {
            throw new CustomException("Room does not have a registered beacon UUID");
        }
        if (room.getSafeRadiusMeters() <= 0) {
            throw new CustomException("Room does not have a valid safe radius configured");
        }
        if (room.getLength() <= 0 || room.getWidth() <= 0) {
            throw new CustomException("Room does not have valid dimensions configured");
        }
    }
}
