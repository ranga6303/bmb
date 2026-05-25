package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@college.com}")
    private String adminEmail;

    @Value("${app.admin.default-password:Admin@123}")
    private String adminDefaultPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            User newAdmin = new User();
            newAdmin.setUsername(adminUsername);
            newAdmin.setEmail(adminEmail);
            newAdmin.setRole(Role.ADMIN);
            newAdmin.setEnabled(true);
            newAdmin.setEmailVerified(true);
            newAdmin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            userRepository.save(newAdmin);
            return;
        }

        if (!isBcrypt(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
            }
            userRepository.save(admin);
        }
    }

    private boolean isBcrypt(String hash) {
        return hash != null && hash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }
}