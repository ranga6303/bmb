package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


public class SmtpEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String mailUsername;
    private final String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender, String mailHost, String mailUsername, String fromAddress) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async
    public void sendPasswordResetOtp(String to, String otp) {
        send(to, "Your password reset OTP", "Your password reset OTP is: " + otp + ". It expires in 15 minutes.");
    }

    @Override
    @Async
    public void sendOtpEmail(String to, String otp) {
        send(to, "Your registration OTP", "Your OTP is: " + otp + ". It expires in 10 minutes.");
    }

    private void send(String to, String subject, String body) {
        logger.info("Sending email using host={}, username={}, fromAddress={}", mailHost, mailUsername, fromAddress);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send email to {}", to, e);
        }
    }
}
