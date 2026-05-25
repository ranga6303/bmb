package com.example.demo.repository;

import com.example.demo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByRoomNumber(String roomNumber);

    Optional<Room> findByBeaconUuid(String beaconUuid);
}
