package com.ipld11stars.backend.dto;

import com.ipld11stars.backend.model.Contest;

import java.math.BigDecimal;

public class ContestDTO {
    private String id;
    private String name;
    private String contestType;
    private String visibility;
    private BigDecimal prizePool;
    private BigDecimal entryFee;
    private String status;
    private String matchId;
    private Integer maxEntries;
    private Integer currentEntries;
    private Integer maxWinners;

    public ContestDTO(Contest contest) {
        this.id = contest.getId();
        this.name = contest.getName();
        this.contestType = contest.getContestType();
        this.visibility = contest.getVisibility();
        this.prizePool = contest.getPrizePool();
        this.entryFee = contest.getEntryFee();
        this.status = contest.getStatus();
        this.matchId = contest.getMatchId();
        this.maxEntries = contest.getMaxEntries();
        this.currentEntries = contest.getCurrentEntries();
        this.maxWinners = contest.getMaxWinners();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContestType() { return contestType; }
    public void setContestType(String contestType) { this.contestType = contestType; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public BigDecimal getPrizePool() { return prizePool; }
    public void setPrizePool(BigDecimal prizePool) { this.prizePool = prizePool; }

    public BigDecimal getEntryFee() { return entryFee; }
    public void setEntryFee(BigDecimal entryFee) { this.entryFee = entryFee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public Integer getMaxEntries() { return maxEntries; }
    public void setMaxEntries(Integer maxEntries) { this.maxEntries = maxEntries; }

    public Integer getCurrentEntries() { return currentEntries; }
    public void setCurrentEntries(Integer currentEntries) { this.currentEntries = currentEntries; }

    public Integer getMaxWinners() { return maxWinners; }
    public void setMaxWinners(Integer maxWinners) { this.maxWinners = maxWinners; }
}