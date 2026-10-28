package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.DeliveryRequest;
import com.ipld11stars.backend.dto.DeliveryResponse;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Delivery;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.repository.jdbc.DeliveryRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.repository.jdbc.MatchSquadRepository;
import com.ipld11stars.backend.scoring.InningsState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ipld11stars.backend.scoring.ScoringRules.*;

/**
 * Records ball-by-ball scoring for a match and enforces the T20 laws we care about:
 * innings order and length, bowler over limits, consecutive overs, free hits, and who can be
 * dismissed how. Every accepted delivery triggers a full recompute of player stats.
 */
@Service
public class DeliveryService {

    private final MatchRepository matchRepository;
    private final MatchSquadRepository squadRepository;
    private final DeliveryRepository deliveryRepository;
    private final PlayerStatsService playerStatsService;
    private final List<MatchProgressListener> listeners;

    public DeliveryService(MatchRepository matchRepository, MatchSquadRepository squadRepository,
                           DeliveryRepository deliveryRepository, PlayerStatsService playerStatsService,
                           List<MatchProgressListener> listeners) {
        this.matchRepository = matchRepository;
        this.squadRepository = squadRepository;
        this.deliveryRepository = deliveryRepository;
        this.playerStatsService = playerStatsService;
        this.listeners = listeners;
    }

    @Transactional
    public DeliveryResponse record(String matchId, DeliveryRequest req) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessRuleException.notFound("Match not found: " + matchId));
        if (!"UPCOMING".equals(match.getMatchStatus()) && !"LIVE".equals(match.getMatchStatus())) {
            throw BusinessRuleException.conflict("Match is " + match.getMatchStatus() + "; no more deliveries can be recorded");
        }
        requireFields(req);

        List<Delivery> existing = deliveryRepository.findByMatchId(matchId);
        InningsState first = replay(existing, 1);
        InningsState second = replay(existing, 2);
        InningsState current = req.innings() == 1 ? first : second;
        Integer target = req.innings() == 2 ? first.getRuns() + 1 : null;

        validateInningsOrder(req, first, second, target);
        String bowlingTeamId = validateTeams(match, req, first, current);
        validateSquads(matchId, req, bowlingTeamId);
        validateOverAndBowler(req, current);
        validateBatters(req, current);
        validateExtras(req);
        boolean freeHit = current.isFreeHitPending() && req.extrasTypeOrNone().isLegalDelivery();
        validateWicket(req, current);

        if ("UPCOMING".equals(match.getMatchStatus())) {
            listeners.forEach(l -> l.onMatchStarting(matchId));
            match.setMatchStatus("LIVE");
            matchRepository.save(match);
        }

        ExtrasType extras = req.extrasTypeOrNone();
        DeliveryRequest.Wicket w = req.wicket();
        Delivery delivery = new Delivery(
                UUID.randomUUID().toString(), matchId, req.innings(), current.getDeliveries() + 1,
                req.overNumber(), req.ballNumber() != null ? req.ballNumber() : current.getLegalBalls() % BALLS_PER_OVER + 1,
                req.battingTeamId(), bowlingTeamId, req.strikerId(), req.nonStrikerId(), req.bowlerId(),
                req.runsBatterOrZero(), extras, req.extrasRunsOrZero(), extras.isLegalDelivery(), freeHit,
                w != null, w != null ? w.dismissalType() : null, w != null ? w.dismissedPlayerId() : null,
                w != null ? w.fielderId() : null, null);
        deliveryRepository.insert(delivery);
        current.apply(delivery);

        playerStatsService.recompute(matchId);
        listeners.forEach(l -> l.onDeliveryRecorded(matchId));

        return new DeliveryResponse(delivery, current.scoreLine(), current.isComplete(target), current.isFreeHitPending());
    }

    /** Innings summaries for the scorecard endpoint. */
    public List<InningsState> getInnings(String matchId) {
        List<Delivery> deliveries = deliveryRepository.findByMatchId(matchId);
        return List.of(replay(deliveries, 1), replay(deliveries, 2)).stream()
                .filter(InningsState::hasStarted)
                .toList();
    }

    /** True when the second innings has ended (all out, overs used up, or target reached). */
    public boolean isMatchResultReached(String matchId) {
        List<Delivery> deliveries = deliveryRepository.findByMatchId(matchId);
        InningsState first = replay(deliveries, 1);
        InningsState second = replay(deliveries, 2);
        return first.isComplete(null) && second.hasStarted() && second.isComplete(first.getRuns() + 1);
    }

    private static InningsState replay(List<Delivery> deliveries, int innings) {
        InningsState state = new InningsState(innings);
        deliveries.stream().filter(d -> d.innings() == innings).forEach(state::apply);
        return state;
    }

    private void requireFields(DeliveryRequest req) {
        if (req == null || req.innings() == null || req.overNumber() == null || req.battingTeamId() == null
                || req.strikerId() == null || req.nonStrikerId() == null || req.bowlerId() == null) {
            throw new BusinessRuleException("innings, overNumber, battingTeamId, strikerId, nonStrikerId and bowlerId are required");
        }
        if (req.innings() != 1 && req.innings() != 2) {
            throw new BusinessRuleException("A T20 match has two innings");
        }
    }

    private void validateInningsOrder(DeliveryRequest req, InningsState first, InningsState second, Integer target) {
        if (req.innings() == 1) {
            if (second.hasStarted()) {
                throw BusinessRuleException.conflict("The second innings has already started");
            }
            if (first.isComplete(null)) {
                throw BusinessRuleException.conflict("The first innings is complete");
            }
        } else {
            if (!first.isComplete(null)) {
                throw BusinessRuleException.conflict("The second innings cannot start until the first innings is complete");
            }
            if (second.isComplete(target)) {
                throw BusinessRuleException.conflict("The second innings is complete");
            }
        }
    }

    private String validateTeams(Match match, DeliveryRequest req, InningsState first, InningsState current) {
        String batting = req.battingTeamId();
        if (!batting.equals(match.getHomeTeamId()) && !batting.equals(match.getAwayTeamId())) {
            throw new BusinessRuleException("Team " + batting + " is not playing in this match");
        }
        if (current.hasStarted() && !batting.equals(current.getBattingTeamId())) {
            throw new BusinessRuleException(current.getBattingTeamId() + " is batting in innings " + req.innings());
        }
        if (req.innings() == 2 && batting.equals(first.getBattingTeamId())) {
            throw new BusinessRuleException(batting + " already batted in the first innings");
        }
        return batting.equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
    }

    private void validateSquads(String matchId, DeliveryRequest req, String bowlingTeamId) {
        List<SquadMember> squad = squadRepository.findByMatchId(matchId);
        Set<String> batting = squad.stream().filter(m -> m.teamId().equals(req.battingTeamId()))
                .map(SquadMember::playerId).collect(Collectors.toSet());
        Set<String> bowling = squad.stream().filter(m -> m.teamId().equals(bowlingTeamId))
                .map(SquadMember::playerId).collect(Collectors.toSet());
        if (batting.isEmpty() || bowling.isEmpty()) {
            throw BusinessRuleException.conflict("Both squads must be set before scoring starts");
        }
        if (!batting.contains(req.strikerId()) || !batting.contains(req.nonStrikerId())) {
            throw new BusinessRuleException("Both batters must be in the batting side's squad");
        }
        if (!bowling.contains(req.bowlerId())) {
            throw new BusinessRuleException("The bowler must be in the fielding side's squad");
        }
        DeliveryRequest.Wicket w = req.wicket();
        if (w != null && w.fielderId() != null && !bowling.contains(w.fielderId())) {
            throw new BusinessRuleException("The fielder must be in the fielding side's squad");
        }
    }

    private void validateOverAndBowler(DeliveryRequest req, InningsState current) {
        int expectedOver = current.currentOverNumber();
        if (req.overNumber() != expectedOver) {
            throw new BusinessRuleException("Expected a delivery in over " + expectedOver + " but got over " + req.overNumber());
        }
        if (current.isOverInProgress()) {
            if (!req.bowlerId().equals(current.getCurrentOverBowlerId())) {
                throw new BusinessRuleException("Over " + expectedOver + " is being bowled by " + current.getCurrentOverBowlerId());
            }
            return;
        }
        if (req.bowlerId().equals(current.getPreviousOverBowlerId())) {
            throw new BusinessRuleException("A bowler cannot bowl consecutive overs");
        }
        if (current.oversBowledBy(req.bowlerId()) >= MAX_OVERS_PER_BOWLER) {
            throw new BusinessRuleException("A bowler can bowl at most " + MAX_OVERS_PER_BOWLER + " overs");
        }
    }

    private void validateBatters(DeliveryRequest req, InningsState current) {
        if (req.strikerId().equals(req.nonStrikerId())) {
            throw new BusinessRuleException("Striker and non-striker must be different players");
        }
        if (current.isDismissed(req.strikerId()) || current.isDismissed(req.nonStrikerId())) {
            throw new BusinessRuleException("A dismissed batter cannot bat again in the same innings");
        }
    }

    private void validateExtras(DeliveryRequest req) {
        ExtrasType extras = req.extrasTypeOrNone();
        int runs = req.runsBatterOrZero();
        int extraRuns = req.extrasRunsOrZero();
        if (runs < 0 || runs > 6 || extraRuns < 0) {
            throw new BusinessRuleException("Runs off the bat must be between 0 and 6 and extras cannot be negative");
        }
        switch (extras) {
            case NONE -> {
                if (extraRuns != 0) throw new BusinessRuleException("extrasRuns must be 0 when there are no extras");
            }
            case WIDE, BYE, LEG_BYE -> {
                if (runs != 0) throw new BusinessRuleException("No runs can be scored off the bat on a " + extras);
                if (extraRuns < 1) throw new BusinessRuleException(extras + " must carry at least one extra run");
            }
            case NO_BALL -> {
                if (extraRuns < 1) throw new BusinessRuleException("A no-ball carries at least the one-run penalty");
            }
        }
    }

    private void validateWicket(DeliveryRequest req, InningsState current) {
        DeliveryRequest.Wicket w = req.wicket();
        if (w == null) {
            return;
        }
        DismissalType type = w.dismissalType();
        if (type == null || w.dismissedPlayerId() == null) {
            throw new BusinessRuleException("A wicket needs a dismissalType and dismissedPlayerId");
        }
        if (type.requiresFielder() && w.fielderId() == null) {
            throw new BusinessRuleException(type + " needs the fielder who completed it");
        }
        ExtrasType extras = req.extrasTypeOrNone();
        if (extras == ExtrasType.NO_BALL && type != DismissalType.RUN_OUT) {
            throw new BusinessRuleException("Only a run out is possible off a no-ball");
        }
        if (extras == ExtrasType.WIDE && type != DismissalType.RUN_OUT && type != DismissalType.STUMPED
                && type != DismissalType.HIT_WICKET) {
            throw new BusinessRuleException("Only run out, stumped or hit wicket are possible off a wide");
        }
        if (current.isFreeHitPending() && extras.isLegalDelivery() && type != DismissalType.RUN_OUT) {
            throw new BusinessRuleException("Only a run out is possible on a free hit");
        }
        boolean strikerOut = w.dismissedPlayerId().equals(req.strikerId());
        boolean nonStrikerOut = w.dismissedPlayerId().equals(req.nonStrikerId());
        if (!strikerOut && !nonStrikerOut) {
            throw new BusinessRuleException("The dismissed player must be one of the two batters");
        }
        if (nonStrikerOut && type != DismissalType.RUN_OUT) {
            throw new BusinessRuleException("Only a run out can dismiss the non-striker");
        }
        if (type == DismissalType.CAUGHT && extras != ExtrasType.NONE) {
            throw new BusinessRuleException("A catch can only be taken off a fair delivery with no extras");
        }
    }
}
