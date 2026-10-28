package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.dto.JoinContestRequest;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.model.EntrySelection;
import com.ipld11stars.backend.security.CurrentUser;
import com.ipld11stars.backend.service.ContestEntryService;
import com.ipld11stars.backend.service.ContestService;
import com.ipld11stars.backend.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contests/{contestId}")
public class ContestEntryController {

    private final ContestEntryService entryService;
    private final ContestService contestService;
    private final LeaderboardService leaderboardService;

    public ContestEntryController(ContestEntryService entryService, ContestService contestService,
                                  LeaderboardService leaderboardService) {
        this.entryService = entryService;
        this.contestService = contestService;
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/entries")
    public ResponseEntity<ContestEntry> join(@PathVariable String contestId, @RequestBody JoinContestRequest request) {
        return ResponseEntity.status(201).body(entryService.join(CurrentUser.requireId(), contestId, request));
    }

    @GetMapping("/entries/me")
    public Map<String, Object> myEntry(@PathVariable String contestId) {
        ContestEntry entry = entryService.getEntry(CurrentUser.requireId(), contestId);
        List<EntrySelection> players = entryService.getSelections(entry.getId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entry", entry);
        body.put("players", players);
        return body;
    }

    @GetMapping("/leaderboard")
    public List<ContestEntry> leaderboard(@PathVariable String contestId) {
        requireContest(contestId);
        return leaderboardService.getLeaderboard(contestId);
    }

    @GetMapping("/prizes")
    public List<ContestPrize> prizes(@PathVariable String contestId) {
        return contestService.getPrizeTable(requireContest(contestId));
    }

    private Contest requireContest(String contestId) {
        return contestService.getContestById(contestId)
                .orElseThrow(() -> BusinessRuleException.notFound("Contest not found: " + contestId));
    }
}
