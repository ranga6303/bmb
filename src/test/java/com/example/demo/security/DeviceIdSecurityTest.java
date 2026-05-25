package com.example.demo.security;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class DeviceIdSecurityTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginPersistsDeviceIdInUserSession() {
        User user = createUser("student1", "student1@test.com", "password", Role.STUDENT);

        LoginRequest request = new LoginRequest();
        request.setUsername(user.getUsername());
        request.setPassword("password");
        request.setDeviceId("device-1");

        HttpServletRequest httpRequest = new MockHttpServletRequest();
        AuthResponse response = authService.login(request, httpRequest);

        List<UserSession> sessions = userSessionRepository.findByUserAndRevokedFalse(user);
        if (sessions.size() != 1) {
            throw new AssertionError("Expected 1 active session but found " + sessions.size());
        }

        UserSession session = sessions.get(0);
        if (!"device-1".equals(session.getDeviceId())) {
            throw new AssertionError("Expected deviceId to be persisted in session");
        }

        String tokenDeviceId = jwtUtil.extractDeviceId(response.getAccessToken());
        if (!"device-1".equals(tokenDeviceId)) {
            throw new AssertionError("Expected deviceId claim to match session");
        }
    }

    @Test
    void deviceIdMismatchIsRejectedAndMatchSucceeds() throws Exception {
        User user = createUser("student2", "student2@test.com", "password", Role.STUDENT);
        UserSession session = createSession(user, "device-a");

        String badToken = issueToken(user, session.getId(), "device-b");
        mockMvc.perform(get("/student/me")
                .header("Authorization", "Bearer " + badToken))
            .andExpect(isUnauthorizedOrForbidden());

        String goodToken = issueToken(user, session.getId(), "device-a");
        mockMvc.perform(get("/student/me")
                .header("Authorization", "Bearer " + goodToken))
            .andExpect(status().isOk());
    }

    private User createUser(String username, String email, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private UserSession createSession(User user, String deviceId) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setRefreshTokenHash("refresh-hash");
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("test");
        session.setLastActiveAt(LocalDateTime.now());
        session.setRevoked(false);
        return userSessionRepository.save(session);
    }

    private String issueToken(User user, Long sessionId, String deviceId) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(new java.util.ArrayList<>())
            .build();

        return jwtUtil.generateToken(
            userDetails,
            user.getId(),
            user.getRole().name(),
            sessionId,
            deviceId
        );
    }

    private ResultMatcher isUnauthorizedOrForbidden() {
        return result -> {
            int status = result.getResponse().getStatus();
            if (status != 401 && status != 403) {
                throw new AssertionError("Expected 401 or 403 but was " + status);
            }
        };
    }
}
