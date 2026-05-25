package com.example.demo.dto;

import java.time.LocalDateTime;

public class ActiveSessionResponse {
    private Long sessionId;
    private String sessionCode;
    private String subjectName;
    private String teacherName;
    private String roomNumber;
    private String beaconUuid;
    private LocalDateTime expiryTime;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
}
