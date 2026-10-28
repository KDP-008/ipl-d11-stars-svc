package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.dto.MatchDTO;
import com.ipld11stars.backend.service.IplApiService;
import com.ipld11stars.backend.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/matches")
@CrossOrigin(origins = "http://localhost:3000")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private IplApiService iplApiService;

    /**
     * Get all matches
     */
    @GetMapping
    public ResponseEntity<List<MatchDTO>> getMatches() {
        List<MatchDTO> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }

    /**
     * Get upcoming matches only
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<MatchDTO>> getUpcomingMatches() {
        List<MatchDTO> matches = matchService.getUpcomingMatches();
        return ResponseEntity.ok(matches);
    }

    /**
     * Get specific match by ID
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<?> getMatch(@PathVariable String matchId) {
        return matchService.getMatchById(matchId)
            .map(match -> ResponseEntity.ok(new MatchDTO(match)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Manually refresh IPL schedule from external API
     * Triggers an immediate sync instead of waiting for the daily scheduled task
     */
    @PostMapping("/sync/refresh")
    public ResponseEntity<Map<String, String>> refreshSchedule() {
        try {
            iplApiService.refreshScheduleNow();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "IPL schedule refresh triggered");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get IPL API service information and status
     */
    @GetMapping("/sync/info")
    public ResponseEntity<String> getApiInfo() {
        String info = iplApiService.getApiProviderInfo();
        return ResponseEntity.ok(info);
    }
}