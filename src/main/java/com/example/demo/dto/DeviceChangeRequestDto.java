package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeviceChangeRequestDto {
    @NotBlank(message = "New device ID is required")
    private String newDeviceId;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    public String getNewDeviceId() { return newDeviceId; }
    public void setNewDeviceId(String newDeviceId) { this.newDeviceId = newDeviceId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
