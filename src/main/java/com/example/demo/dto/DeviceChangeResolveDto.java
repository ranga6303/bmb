package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class DeviceChangeResolveDto {
    @Size(max = 500, message = "Admin remarks must be at most 500 characters")
    private String adminRemarks;

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
}
