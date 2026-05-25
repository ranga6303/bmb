package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
