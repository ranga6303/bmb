package com.example.demo.service;

import com.example.demo.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendPasswordResetOtp(String to, String otp) {
        log.error("NoOp email service is active. Password reset OTP was not sent to={}", to);
        throw new CustomException("Email service is not configured. Please contact admin.");
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        log.error("NoOp email service is active. Registration OTP was not sent to={}", to);
        throw new CustomException("Email service is not configured. Please contact admin.");
    }
}
