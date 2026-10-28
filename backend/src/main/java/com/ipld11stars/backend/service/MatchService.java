package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.MatchDTO;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    @Autowired
    private MatchRepository matchRepository;

    /**
     * Get all matches with caching
     * Cache is invalidated when matches are updated
     */
    @Cacheable(value = "ipl-schedule", key = "'all-matches'")
    public List<MatchDTO> getAllMatches() {
        logger.info("Fetching all matches (from database)");
        List<Match> matches = matchRepository.findAll();
        return matches.stream()
                .map(MatchDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming matches with caching
     * Returns only matches with UPCOMING status and future start times
     */
    @Cacheable(value = "ipl-schedule", key = "'upcoming-matches'")
    public List<MatchDTO> getUpcomingMatches() {
        logger.info("Fetching upcoming matches (from database)");
        List<Match> matches = matchRepository.findUpcoming();
        return matches.stream()
                .map(MatchDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get specific match by ID with caching
     */
    @Cacheable(value = "match-details", key = "#id")
    public Optional<Match> getMatchById(String id) {
        logger.info("Fetching match with ID: {}", id);
        return matchRepository.findById(id);
    }

    /**
     * Create a new match
     * Invalidates the cache so upcoming calls fetch fresh data
     */
    @CacheEvict(value = "ipl-schedule", allEntries = true)
    public Match createMatch(Integer matchNumber, String homeTeamId, String awayTeamId,
                            String seasonId, LocalDateTime startTimeUtc, String venue) {
        logger.info("Creating match: {} vs {} at {}", homeTeamId, awayTeamId, venue);
        Match match = new Match(matchNumber, homeTeamId, awayTeamId, seasonId, startTimeUtc, venue);
        return matchRepository.save(match);
    }

    /**
     * Create or update a match (used by IPL sync service)
     * If match number + season already exists, update it; otherwise create new
     */
    @CacheEvict(value = "ipl-schedule", allEntries = true)
    public Match createOrUpdateMatch(Match match) {
        try {
            // Try to find existing match by match number and season
            Optional<Match> existing = matchRepository.findByMatchNumberAndSeason(
                    match.getMatchNumber(), 
                    match.getSeasonId()
            );

            if (existing.isPresent()) {
                Match existingMatch = existing.get();
                // Update fields
                existingMatch.setHomeTeamId(match.getHomeTeamId());
                existingMatch.setAwayTeamId(match.getAwayTeamId());
                existingMatch.setVenue(match.getVenue());
                existingMatch.setStartTimeUtc(match.getStartTimeUtc());
                existingMatch.setMatchStatus(match.getMatchStatus());
                existingMatch.setMatchType(match.getMatchType());
                existingMatch.setUpdatedAt(LocalDateTime.now());

                logger.info("Updating match {}: {} vs {}", match.getMatchNumber(), 
                        match.getHomeTeamId(), match.getAwayTeamId());
                return matchRepository.save(existingMatch);
            } else {
                // Create new match
                match.setCreatedAt(LocalDateTime.now());
                match.setUpdatedAt(LocalDateTime.now());
                logger.info("Creating new match {}: {} vs {}", match.getMatchNumber(),
                        match.getHomeTeamId(), match.getAwayTeamId());
                return matchRepository.save(match);
            }
        } catch (Exception e) {
            logger.error("Error creating/updating match: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update match status (e.g., when match is live or completed)
     */
    @CacheEvict(value = "ipl-schedule", allEntries = true)
    public Match updateMatchStatus(String matchId, String status) {
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            match.setMatchStatus(status);
            match.setUpdatedAt(LocalDateTime.now());
            logger.info("Updated match {} status to: {}", matchId, status);
            return matchRepository.save(match);
        }
        return null;
    }
}