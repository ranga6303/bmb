package com.example.demo.service;

import com.example.demo.exception.ServiceUnavailableException;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final String apiKey;
    private final String fromAddress;

    public SmtpEmailService(String apiKey, String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetOtp(String to, String otp) {
        send(to, "Your password reset OTP", "Your password reset OTP is: " + otp + ". It expires in 15 minutes.");
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        send(to, "Your registration OTP", "Your OTP is: " + otp + ". It expires in 10 minutes.");
    }

    private void send(String to, String subject, String body) {
        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(to)
                    .subject(subject)
                    .text(body)
                    .build();
            resend.emails().send(params);
            logger.info("Email sent via Resend API to={}", to);
        } catch (Exception e) {
            logger.error("Failed to send email via Resend to {}", to, e);
            throw new ServiceUnavailableException("Failed to send email. Please try again later.", e);
        }
    }
}
