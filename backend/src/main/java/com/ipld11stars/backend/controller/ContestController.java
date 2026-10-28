package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.dto.ContestDTO;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.service.ContestService;
import com.ipld11stars.backend.service.MatchService;
import com.ipld11stars.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/contests")
@CrossOrigin(origins = "http://localhost:3000")
public class ContestController {

    @Autowired
    private ContestService contestService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<ContestDTO>> getContests() {
        List<ContestDTO> contests = contestService.getAllContests();
        return ResponseEntity.ok(contests);
    }

    @GetMapping("/{contestId}")
    public ResponseEntity<?> getContest(@PathVariable String contestId) {
        Optional<Contest> contestOpt = contestService.getContestById(contestId);
        if (contestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Contest contest = contestOpt.get();
        ContestDTO contestDTO = new ContestDTO(contest);
        return ResponseEntity.ok(contestDTO);
    }

    private String resolveUserId(String headerUserId) {
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId.trim();
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createContest(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                          @RequestBody CreateContestRequest request) {
        try {
            userId = resolveUserId(userId);
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User authentication required"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<Match> matchOpt = matchService.getMatchById(request.getMatchId());
            if (matchOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Match not found"));
            }

            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Contest name is required"));
            }
            if (request.getMaxEntries() == null || request.getMaxEntries() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Max entries must be greater than 0"));
            }

            Contest contest = contestService.createContest(
                request.getName(),
                request.getMatchId(),
                userId,
                request.getContestType() != null ? request.getContestType() : "FREE",
                request.getMaxEntries(),
                request.getEntryFee() != null ? request.getEntryFee() : BigDecimal.ZERO,
                request.getPrizePool() != null ? request.getPrizePool() : BigDecimal.ZERO,
                request.getMaxWinners() != null ? request.getMaxWinners() : 1,
                request.getMinEntries(),
                request.getPrizes()
            );

            Map<String, Object> response = Map.of(
                "id", contest.getId(),
                "name", contest.getName(),
                "matchId", contest.getMatchId(),
                "contestType", contest.getContestType(),
                "visibility", contest.getVisibility(),
                "prizePool", contest.getPrizePool(),
                "entryFee", contest.getEntryFee(),
                "maxWinners", contest.getMaxWinners(),
                "status", contest.getStatus(),
                "createdByUserId", contest.getCreatedByUserId()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{contestId}")
    public ResponseEntity<?> updateContest(@PathVariable String contestId,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId,
                                          @RequestBody UpdateContestRequest request) {
        try {
            userId = resolveUserId(userId);
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User authentication required"));
            }

            // Check if user is the contest creator
            if (!contestService.canUserEditContest(contestId, userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only contest creator can edit the contest"));
            }

            Contest updatedContest = contestService.updateContest(
                contestId,
                userId,
                request.getName(),
                request.getContestType(),
                request.getMaxEntries(),
                request.getEntryFee(),
                request.getPrizePool(),
                request.getMaxWinners()
            );

            Map<String, Object> response = Map.of(
                "id", updatedContest.getId(),
                "name", updatedContest.getName(),
                "message", "Contest updated successfully"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{contestId}")
    public ResponseEntity<?> deleteContest(@PathVariable String contestId,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            userId = resolveUserId(userId);
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User authentication required"));
            }

            // Check if user is the contest creator
            if (!contestService.canUserEditContest(contestId, userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only contest creator can delete the contest"));
            }

            contestService.deleteContest(contestId, userId);

            return ResponseEntity.ok(Map.of("message", "Contest deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserContests(@PathVariable String userId) {
        try {
            List<Contest> contests = contestService.getContestsByUserId(userId);
            List<ContestDTO> dtos = contests.stream().map(ContestDTO::new).toList();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Request DTOs
    public static class CreateContestRequest {
        private String name;
        private String contestType;
        private Integer maxEntries;
        private BigDecimal entryFee;
        private BigDecimal prizePool;
        private Integer maxWinners;
        private String matchId;
        private Integer minEntries;
        private List<ContestPrize> prizes;

        // Getters and Setters
        public Integer getMinEntries() { return minEntries; }
        public void setMinEntries(Integer minEntries) { this.minEntries = minEntries; }
        public List<ContestPrize> getPrizes() { return prizes; }
        public void setPrizes(List<ContestPrize> prizes) { this.prizes = prizes; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getContestType() { return contestType; }
        public void setContestType(String contestType) { this.contestType = contestType; }
        public Integer getMaxEntries() { return maxEntries; }
        public void setMaxEntries(Integer maxEntries) { this.maxEntries = maxEntries; }
        public BigDecimal getEntryFee() { return entryFee; }
        public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }
        public BigDecimal getPrizePool() { return prizePool; }
        public void setPrizePool(BigDecimal prizePool) { this.prizePool = prizePool; }
        public Integer getMaxWinners() { return maxWinners; }
        public void setMaxWinners(Integer maxWinners) { this.maxWinners = maxWinners; }
        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }
    }

    public static class UpdateContestRequest {
        private String name;
        private String contestType;
        private Integer maxEntries;
        private BigDecimal entryFee;
        private BigDecimal prizePool;
        private Integer maxWinners;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getContestType() { return contestType; }
        public void setContestType(String contestType) { this.contestType = contestType; }
        public Integer getMaxEntries() { return maxEntries; }
        public void setMaxEntries(Integer maxEntries) { this.maxEntries = maxEntries; }
        public BigDecimal getEntryFee() { return entryFee; }
        public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }
        public BigDecimal getPrizePool() { return prizePool; }
        public void setPrizePool(BigDecimal prizePool) { this.prizePool = prizePool; }
        public Integer getMaxWinners() { return maxWinners; }
        public void setMaxWinners(Integer maxWinners) { this.maxWinners = maxWinners; }
    }
}