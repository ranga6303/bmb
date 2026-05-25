package com.example.demo.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    private final PasswordEncoder passwordEncoder;

    public TestController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test-password")
    public Map<String, Object> testPassword(
        @RequestParam String password, 
        @RequestParam String hash
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", passwordEncoder.matches(password, hash));
        
        // Generate a new hash for the password
        String newHash = passwordEncoder.encode(password);
        result.put("newHash", newHash);
        result.put("newHashMatches", passwordEncoder.matches(password, newHash));
        
        return result;
    }
}
