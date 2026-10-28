package com.ipld11stars.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /auth/register should successfully register a new user")
    void testRegisterSuccess() throws Exception {
        String uniqueUser = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Rohit",
                "lastName", "Sharma",
                "userId", uniqueUser,
                "password", "mumbai123"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(uniqueUser))
                .andExpect(jsonPath("$.firstName").value("Rohit"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("POST /auth/register should reject duplicate user ID")
    void testRegisterDuplicateFails() throws Exception {
        // 'testplayer' is seeded by TestDataSeeder
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Duplicate",
                "userId", "testplayer",
                "password", "secret123"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User ID already taken"));
    }

    @Test
    @DisplayName("POST /auth/register should validate minimum password length")
    void testRegisterShortPasswordFails() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Short",
                "userId", "shortuser",
                "password", "123"
        ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be at least 6 characters"));
    }

    @Test
    @DisplayName("POST /auth/login with valid seeded credentials should return JWT token")
    void testLoginSuccess() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "userId", "testplayer",
                "password", "password123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.userId").value("testplayer"))
                .andExpect(jsonPath("$.user.role").value("PLAYER"));
    }

    @Test
    @DisplayName("POST /auth/login with wrong password should fail with 400")
    void testLoginInvalidPasswordFails() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "userId", "testplayer",
                "password", "wrongpassword"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid user ID or password"));
    }

    @Test
    @DisplayName("GET /auth/me with Bearer token should return user profile")
    void testAuthMeWithTokenSuccess() throws Exception {
        String token = obtainAccessToken("testplayer", "password123");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("testplayer"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    @DisplayName("GET /auth/me without Bearer token should fail with 401 or 403")
    void testAuthMeWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isForbidden());
    }
}
