package com.ipld11stars.backend.model;

import java.time.LocalDateTime;

public class Match {
    private String id;
    private Integer matchNumber;
    private String homeTeamId;
    private String awayTeamId;
    private String seasonId;
    private String venue;
    private LocalDateTime startTimeUtc;
    private String matchStatus;
    private String matchType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Match() {}

    public Match(Integer matchNumber, String homeTeamId, String awayTeamId, String seasonId,
                 LocalDateTime startTimeUtc, String venue) {
        this.matchNumber = matchNumber;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.seasonId = seasonId;
        this.startTimeUtc = startTimeUtc;
        this.venue = venue;
        this.matchStatus = "UPCOMING";
        this.matchType = "LEAGUE";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getMatchNumber() {
        return matchNumber;
    }

    public void setMatchNumber(Integer matchNumber) {
        this.matchNumber = matchNumber;
    }

    public String getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(String homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(String awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public LocalDateTime getStartTimeUtc() {
        return startTimeUtc;
    }

    public void setStartTimeUtc(LocalDateTime startTimeUtc) {
        this.startTimeUtc = startTimeUtc;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
