package com.example.demo.config;

import com.example.demo.service.EmailService;
import com.example.demo.service.NoOpEmailService;
import com.example.demo.service.SmtpEmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfiguration {
    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public EmailService smtpEmailService(JavaMailSender javaMailSender) {
        return new SmtpEmailService(javaMailSender);
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService noOpEmailService() {
        return new NoOpEmailService();
    }
}
