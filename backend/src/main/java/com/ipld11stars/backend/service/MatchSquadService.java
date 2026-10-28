package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.SquadRequest;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.Player;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.repository.jdbc.MatchSquadRepository;
import com.ipld11stars.backend.repository.jdbc.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MatchSquadService {

    private static final int STARTING_XI_SIZE = 11;

    private final MatchRepository matchRepository;
    private final MatchSquadRepository squadRepository;
    private final PlayerRepository playerRepository;

    public MatchSquadService(MatchRepository matchRepository, MatchSquadRepository squadRepository,
                             PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.squadRepository = squadRepository;
        this.playerRepository = playerRepository;
    }

    public List<SquadMember> getSquad(String matchId) {
        requireMatch(matchId);
        return squadRepository.findByMatchId(matchId);
    }

    @Transactional
    public List<SquadMember> setSquad(String matchId, SquadRequest request) {
        Match match = requireMatch(matchId);
        if (!"UPCOMING".equals(match.getMatchStatus())) {
            throw BusinessRuleException.conflict("Squads can only be changed before the match starts");
        }
        if (request == null || request.teamId() == null) {
            throw new BusinessRuleException("teamId is required");
        }
        if (!request.teamId().equals(match.getHomeTeamId()) && !request.teamId().equals(match.getAwayTeamId())) {
            throw new BusinessRuleException("Team " + request.teamId() + " is not playing in this match");
        }
        List<String> playerIds = request.playerIds() != null ? request.playerIds() : List.of();
        Set<String> squad = new HashSet<>(playerIds);
        if (squad.size() != playerIds.size()) {
            throw new BusinessRuleException("Squad contains duplicate players");
        }
        if (squad.size() < STARTING_XI_SIZE) {
            throw new BusinessRuleException("A squad needs at least " + STARTING_XI_SIZE + " players");
        }
        Set<String> startingXi = new HashSet<>(request.startingXi() != null ? request.startingXi() : List.of());
        if (!startingXi.isEmpty() && startingXi.size() != STARTING_XI_SIZE) {
            throw new BusinessRuleException("Starting XI must have exactly " + STARTING_XI_SIZE + " players");
        }
        if (!squad.containsAll(startingXi)) {
            throw new BusinessRuleException("Every starting XI player must be in the squad");
        }
        for (String playerId : playerIds) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new BusinessRuleException("Unknown player " + playerId));
            if (!request.teamId().equals(player.teamId())) {
                throw new BusinessRuleException("Player " + playerId + " does not play for " + request.teamId());
            }
        }

        squadRepository.deleteByMatchAndTeam(matchId, request.teamId());
        for (String playerId : playerIds) {
            squadRepository.insert(matchId, playerId, request.teamId(), startingXi.contains(playerId));
        }
        return squadRepository.findByMatchId(matchId);
    }

    private Match requireMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessRuleException.notFound("Match not found: " + matchId));
    }
}
