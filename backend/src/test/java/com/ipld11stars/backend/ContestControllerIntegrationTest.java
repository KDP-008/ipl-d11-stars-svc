package com.ipld11stars.backend;

import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ContestControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    @DisplayName("GET /contests should return list of seeded contests")
    void testGetAllContests() throws Exception {
        mockMvc.perform(get("/contests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].contestType").isString());
    }

    @Test
    @DisplayName("GET /contests/{id} should return contest details")
    void testGetContestById() throws Exception {
        Contest existing = contestRepository.findAll().get(0);

        mockMvc.perform(get("/contests/" + existing.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.name").value(existing.getName()));
    }

    @Test
    @DisplayName("POST /contests/create without token should be rejected (401/403)")
    void testCreateContestUnauthenticatedFails() throws Exception {
        String matchId = matchRepository.findAll().get(0).getId();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Unauthorized Contest",
                "matchId", matchId,
                "maxEntries", 10
        ));

        mockMvc.perform(post("/contests/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /contests/create with valid Bearer token should succeed")
    void testCreateContestAuthenticatedSuccess() throws Exception {
        String token = obtainAccessToken("testplayer", "password123");
        String matchId = matchRepository.findAll().get(0).getId();

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Test Player League",
                "matchId", matchId,
                "maxEntries", 20,
                "contestType", "FREE",
                "entryFee", BigDecimal.ZERO,
                "prizePool", BigDecimal.valueOf(500),
                "maxWinners", 2
        ));

        mockMvc.perform(post("/contests/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Player League"))
                .andExpect(jsonPath("$.maxWinners").value(2))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    @DisplayName("PUT /contests/{id} and DELETE /contests/{id} by contest creator should succeed")
    void testUpdateAndDeleteContestLifecycle() throws Exception {
        String token = obtainAccessToken("contestadmin", "admin123");
        String matchId = matchRepository.findAll().get(0).getId();

        // 1. Create a contest
        String createJson = objectMapper.writeValueAsString(Map.of(
                "name", "Temporary Contest",
                "matchId", matchId,
                "maxEntries", 5
        ));

        String responseStr = mockMvc.perform(post("/contests/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String contestId = (String) objectMapper.readValue(responseStr, Map.class).get("id");

        // 2. Update the contest
        String updateJson = objectMapper.writeValueAsString(Map.of(
                "name", "Updated Temporary Contest",
                "maxEntries", 8,
                "contestType", "FREE",
                "entryFee", BigDecimal.ZERO,
                "prizePool", BigDecimal.ZERO,
                "maxWinners", 1
        ));

        mockMvc.perform(put("/contests/" + contestId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Temporary Contest"));

        // 3. Delete the contest
        mockMvc.perform(delete("/contests/" + contestId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contest deleted successfully"));

        // 4. Verify it is gone
        mockMvc.perform(get("/contests/" + contestId))
                .andExpect(status().isNotFound());
    }
}
