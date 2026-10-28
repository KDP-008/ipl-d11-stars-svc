package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.dto.DeliveryRequest;
import com.ipld11stars.backend.dto.DeliveryResponse;
import com.ipld11stars.backend.dto.SquadRequest;
import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.scoring.InningsState;
import com.ipld11stars.backend.service.DeliveryService;
import com.ipld11stars.backend.service.MatchSquadService;
import com.ipld11stars.backend.service.PlayerStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Squads and live ball-by-ball scoring. Writes are admin-only (see SecurityConfig).
 */
@RestController
@RequestMapping("/matches/{matchId}")
public class MatchScoringController {

    private final MatchSquadService squadService;
    private final DeliveryService deliveryService;
    private final PlayerStatsService playerStatsService;

    public MatchScoringController(MatchSquadService squadService, DeliveryService deliveryService,
                                  PlayerStatsService playerStatsService) {
        this.squadService = squadService;
        this.deliveryService = deliveryService;
        this.playerStatsService = playerStatsService;
    }

    @GetMapping("/squad")
    public List<SquadMember> getSquad(@PathVariable String matchId) {
        return squadService.getSquad(matchId);
    }

    @PutMapping("/squad")
    public List<SquadMember> setSquad(@PathVariable String matchId, @RequestBody SquadRequest request) {
        return squadService.setSquad(matchId, request);
    }

    @PostMapping("/deliveries")
    public ResponseEntity<DeliveryResponse> recordDelivery(@PathVariable String matchId,
                                                           @RequestBody DeliveryRequest request) {
        return ResponseEntity.status(201).body(deliveryService.record(matchId, request));
    }

    @GetMapping("/scorecard")
    public List<Map<String, Object>> getScorecard(@PathVariable String matchId) {
        return deliveryService.getInnings(matchId).stream().map(MatchScoringController::summary).toList();
    }

    @GetMapping("/player-stats")
    public List<PlayerMatchStats> getPlayerStats(@PathVariable String matchId) {
        return playerStatsService.getStats(matchId);
    }

    private static Map<String, Object> summary(InningsState s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("innings", s.getInnings());
        m.put("battingTeamId", s.getBattingTeamId());
        m.put("runs", s.getRuns());
        m.put("wickets", s.getWickets());
        m.put("legalBalls", s.getLegalBalls());
        m.put("score", s.scoreLine());
        return m;
    }
}
