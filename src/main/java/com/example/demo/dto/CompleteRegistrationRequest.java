package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompleteRegistrationRequest {
    @NotBlank
    @Size(max = 100)
    private String collegeId;

    @NotBlank
    @Size(max = 100)
    private String otp;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;

    public String getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(String collegeId) {
        this.collegeId = collegeId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
