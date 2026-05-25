package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public class CreateSessionRequest {
    @NotNull
    private Long subjectId;

    @NotNull
    private Long sectionId;

    @Size(max = 50)
    private String roomNumber;

    @Size(max = 100)
    private String beaconUuid;

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

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

    @AssertTrue(message = "Either roomNumber or beaconUuid is required")
    public boolean isLocationProvided() {
        return (roomNumber != null && !roomNumber.isBlank()) || (beaconUuid != null && !beaconUuid.isBlank());
    }
}
