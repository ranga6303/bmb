package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendPasswordResetOtp(String to, String otp) {
        log.info("NoOp email: password-reset-otp to={} otp={}", to, otp);
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        log.info("NoOp email: otp to={} otp={}", to, otp);
    }
}
