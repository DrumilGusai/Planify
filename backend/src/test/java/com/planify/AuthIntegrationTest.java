package com.planify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshAndReuseDetectionFlow() throws Exception {
        String email = "auth-flow@planify.local";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Auth User","email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("rt"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie firstRefreshCookie = loginResult.getResponse().getCookie("rt");
        assertThat(firstRefreshCookie).isNotNull();
        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Rotate refresh token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("rt"))
                .andReturn();

        Cookie secondRefreshCookie = refreshResult.getResponse().getCookie("rt");
        assertThat(secondRefreshCookie).isNotNull();
        assertThat(secondRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());

        // Reuse the old refresh token -> 401 and revokes all user tokens (including the second one)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(firstRefreshCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(secondRefreshCookie))
                .andExpect(status().isUnauthorized());
    }
}
