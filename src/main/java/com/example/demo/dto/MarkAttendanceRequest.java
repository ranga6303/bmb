package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MarkAttendanceRequest {
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Session code must be exactly 6 digits")
    private String sessionCode;

    @NotBlank
    private String beaconUuid;

    @NotBlank
    private String deviceId;

    @Size(max = 100)
    private String androidId;

    @NotBlank
    private String deviceSignature;

    @NotBlank
    private String signedPayload;

    private String publicKey;

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getBeaconUuid() {
        return beaconUuid;
    }

    public void setBeaconUuid(String beaconUuid) {
        this.beaconUuid = beaconUuid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getDeviceSignature() {
        return deviceSignature;
    }

    public void setDeviceSignature(String deviceSignature) {
        this.deviceSignature = deviceSignature;
    }

    public String getSignedPayload() {
        return signedPayload;
    }

    public void setSignedPayload(String signedPayload) {
        this.signedPayload = signedPayload;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
