package com.ipld11stars.backend.dto;

import com.ipld11stars.backend.model.Match;

import java.time.LocalDateTime;

public class MatchDTO {
    private String id;
    private Integer matchNumber;
    private String matchStatus;
    private String matchType;
    private LocalDateTime startTimeUtc;
    private String venue;
    private String homeTeamId;
    private String awayTeamId;
    private String seasonId;

    public MatchDTO(Match match) {
        this.id = match.getId();
        this.matchNumber = match.getMatchNumber();
        this.matchStatus = match.getMatchStatus();
        this.matchType = match.getMatchType();
        this.startTimeUtc = match.getStartTimeUtc();
        this.venue = match.getVenue();
        this.homeTeamId = match.getHomeTeamId();
        this.awayTeamId = match.getAwayTeamId();
        this.seasonId = match.getSeasonId();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getMatchNumber() { return matchNumber; }
    public void setMatchNumber(Integer matchNumber) { this.matchNumber = matchNumber; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public LocalDateTime getStartTimeUtc() { return startTimeUtc; }
    public void setStartTimeUtc(LocalDateTime startTimeUtc) { this.startTimeUtc = startTimeUtc; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(String homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(String awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getSeasonId() { return seasonId; }
    public void setSeasonId(String seasonId) { this.seasonId = seasonId; }
}