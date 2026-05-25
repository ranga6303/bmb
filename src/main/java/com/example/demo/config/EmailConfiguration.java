package com.example.demo.config;

import com.example.demo.service.EmailService;
import com.example.demo.service.NoOpEmailService;
import com.example.demo.service.SmtpEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EmailConfiguration.class);

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public EmailService smtpEmailService(JavaMailSender javaMailSender) {
        logger.info("SMTP email service enabled host={} username={} fromAddress={}", mailHost, mailUsername, fromAddress);
        return new SmtpEmailService(javaMailSender, mailHost, mailUsername, fromAddress);
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService noOpEmailService() {
        logger.error("NoOp email service enabled. Emails will not be sent because SMTP is not configured.");
        return new NoOpEmailService();
    }
}
