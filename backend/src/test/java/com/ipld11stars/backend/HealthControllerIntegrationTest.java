package com.ipld11stars.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class HealthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /health should return 200 and UP status")
    void testHealthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ipl-d11-stars-backend"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }
}
