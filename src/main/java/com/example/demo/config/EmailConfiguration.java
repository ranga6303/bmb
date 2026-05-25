package com.example.demo.config;

import com.example.demo.service.EmailService;
import com.example.demo.service.NoOpEmailService;
import com.example.demo.service.SmtpEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfiguration {
    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public EmailService smtpEmailService(JavaMailSender javaMailSender) {
        return new SmtpEmailService(javaMailSender, mailHost, mailUsername, fromAddress);
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService noOpEmailService() {
        return new NoOpEmailService();
    }
}
