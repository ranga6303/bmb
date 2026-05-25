package com.example.demo.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/greet")
public class GreetingController {
    @GetMapping
    public String greet(Authentication authentication) {
        String permissions = authentication.getAuthorities().stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
        return "Hello " + authentication.getName() + ", your permissions are: " + permissions;
    }
}
