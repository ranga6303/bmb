package com.example.demo.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class TestPasswordSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testPasswordUnauthenticatedIsDenied() throws Exception {
        String hash = passwordEncoder.encode("pass");
        mockMvc.perform(get("/test-password")
                .param("password", "pass")
                .param("hash", hash))
            .andExpect(isUnauthorizedOrForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testPasswordNonAdminIsForbidden() throws Exception {
        String hash = passwordEncoder.encode("pass");
        mockMvc.perform(get("/test-password")
                .param("password", "pass")
                .param("hash", hash))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testPasswordAdminIsAllowed() throws Exception {
        String hash = passwordEncoder.encode("pass");
        mockMvc.perform(get("/test-password")
                .param("password", "pass")
                .param("hash", hash))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(true));
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
