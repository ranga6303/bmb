package com.example.demo.auth;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.CompleteRegistrationRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.InitiateRegistrationRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/initiate-registration")
    public ResponseEntity<MessageResponse> initiateRegistration(@Valid @RequestBody InitiateRegistrationRequest request) {
        return ResponseEntity.ok(authService.initiateRegistration(request));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<MessageResponse> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request) {
        return ResponseEntity.ok(authService.completeRegistration(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.login(request, httpServletRequest));
    }

    @PostMapping("/login/student")
    public ResponseEntity<AuthResponse> loginStudent(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.loginStudent(request, httpServletRequest));
    }

    @PostMapping("/login/staff")
    public ResponseEntity<AuthResponse> loginStaff(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.loginStaff(request, httpServletRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request.getCollegeId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken(), httpServletRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String token = request.getCollegeId() + "::" + request.getOtp();
        return ResponseEntity.ok(authService.resetPassword(token, request.getNewPassword()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.logout(request.getRefreshToken()));
    }
}
