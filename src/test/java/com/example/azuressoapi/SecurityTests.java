package com.example.azuressoapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for security configuration and authentication flow.
 * 
 * These tests verify that:
 * 1. Protected endpoints return 401 without authentication
 * 2. Protected endpoints return 200 with valid JWT token
 * 3. User profile endpoints correctly extract JWT claims
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.cloud.azure.active-directory.enabled=false"
})
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that protected endpoints return 401 Unauthorized without authentication.
     */
    @Test
    void testHomeEndpoint_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserProfileEndpoint_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testStatusEndpoint_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Test that protected endpoints return 200 OK with valid JWT mock.
     */
    @Test
    void testHomeEndpoint_WithJwt_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/home")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("sub", "test-user-id")
                        .claim("name", "Test User")
                        .claim("email", "test@example.com")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testHomeJwtEndpoint_WithJwt_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/home/jwt")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("sub", "test-user-id")
                        .claim("name", "Test User")
                        .claim("email", "test@example.com")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.subject").value("test-user-id"));
    }

    /**
     * Test that user profile endpoint correctly extracts JWT claims.
     */
    @Test
    void testUserProfile_WithJwt_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("oid", "12345-67890-abcdef")
                        .claim("name", "John Doe")
                        .claim("email", "john.doe@example.com")
                        .claim("preferred_username", "john.doe@example.com")
                        .claim("given_name", "John")
                        .claim("family_name", "Doe")
                        .claim("tid", "tenant-id-123")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.oid").value("12345-67890-abcdef"))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.preferredUsername").value("john.doe@example.com"))
            .andExpect(jsonPath("$.givenName").value("John"))
            .andExpect(jsonPath("$.familyName").value("Doe"))
            .andExpect(jsonPath("$.tenantId").value("tenant-id-123"));
    }

    /**
     * Test that claims endpoint returns all JWT claims.
     */
    @Test
    void testUserClaims_WithJwt_ReturnsAllClaims() throws Exception {
        mockMvc.perform(get("/api/v1/user/claims")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("oid", "12345-67890-abcdef")
                        .claim("name", "Test User")
                        .claim("email", "test@example.com")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.claims").exists())
            .andExpect(jsonPath("$.claims.oid").value("12345-67890-abcdef"))
            .andExpect(jsonPath("$.claims.name").value("Test User"));
    }

    /**
     * Test that roles endpoint returns user roles and groups.
     */
    @Test
    void testUserRoles_WithJwt_ReturnsRoles() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("groups", java.util.List.of("group1", "group2"))
                        .claim("roles", java.util.List.of("User", "Admin"))
                        .claim("scp", "User.Read email profile")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groups").isArray())
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.scopes").value("User.Read email profile"));
    }

    /**
     * Test status endpoint with authentication.
     */
    @Test
    void testStatusEndpoint_WithJwt_ReturnsAuthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/status")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .claim("sub", "test-user-id")
                        .claim("name", "Test User")
                    )
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.principal").exists());
    }

    /**
     * Test health check endpoint is publicly accessible.
     */
    @Test
    void testHealthEndpoint_WithoutAuth_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }
}
