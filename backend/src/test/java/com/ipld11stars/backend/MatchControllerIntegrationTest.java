package com.ipld11stars.backend;

import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MatchControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MatchRepository matchRepository;

    @Test
    @DisplayName("GET /matches should return list of seeded matches")
    void testGetAllMatches() throws Exception {
        mockMvc.perform(get("/matches")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].matchNumber").isNumber())
                .andExpect(jsonPath("$[0].homeTeamId").isString())
                .andExpect(jsonPath("$[0].awayTeamId").isString());
    }

    @Test
    @DisplayName("GET /matches/upcoming should return upcoming matches")
    void testGetUpcomingMatches() throws Exception {
        mockMvc.perform(get("/matches/upcoming")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /matches/{id} should return match details for valid ID")
    void testGetMatchById() throws Exception {
        String existingMatchId = matchRepository.findAll().get(0).getId();

        mockMvc.perform(get("/matches/" + existingMatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingMatchId))
                .andExpect(jsonPath("$.venue").isString());
    }

    @Test
    @DisplayName("GET /matches/{id} with unknown ID should return 404 Not Found")
    void testGetMatchByIdNotFound() throws Exception {
        mockMvc.perform(get("/matches/unknown-match-id-12345"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /matches/sync/info should return provider info")
    void testGetSyncInfo() throws Exception {
        mockMvc.perform(get("/matches/sync/info"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IPL Schedule Sync Service")));
    }
}
