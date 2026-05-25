package com.example.demo.service;

public interface EmailService {
    void sendPasswordResetOtp(String to, String otp);

    void sendOtpEmail(String to, String otp);
}
