package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false, unique = true, length = 100)
    private String beaconUuid;

    private double length;
    private double width;
    private double safeRadiusMeters;

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getBeaconUuid() {
        return beaconUuid;
    }

    public void setBeaconUuid(String beaconUuid) {
        this.beaconUuid = beaconUuid;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getSafeRadiusMeters() {
        return safeRadiusMeters;
    }

    public void setSafeRadiusMeters(double safeRadiusMeters) {
        this.safeRadiusMeters = safeRadiusMeters;
    }
}
