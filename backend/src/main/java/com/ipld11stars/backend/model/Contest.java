package com.ipld11stars.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Contest {
    private String id;
    private String name;
    private String matchId;
    private String createdByUserId;
    private String contestType;
    private String visibility;
    private Integer maxEntries;
    private Integer currentEntries;
    private BigDecimal entryFee;
    private BigDecimal prizePool;
    private Integer maxWinners;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Contest() {}

    public Contest(String name, String matchId, String createdByUserId, String contestType,
                   Integer maxEntries, BigDecimal entryFee, BigDecimal prizePool, Integer maxWinners) {
        this.name = name;
        this.matchId = matchId;
        this.createdByUserId = createdByUserId;
        this.contestType = contestType;
        this.maxEntries = maxEntries;
        this.currentEntries = 0;
        this.entryFee = entryFee;
        this.prizePool = prizePool;
        this.maxWinners = maxWinners;
        this.visibility = "PUBLIC";
        this.status = "OPEN";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getContestType() {
        return contestType;
    }

    public void setContestType(String contestType) {
        this.contestType = contestType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Integer getCurrentEntries() {
        return currentEntries;
    }

    public void setCurrentEntries(Integer currentEntries) {
        this.currentEntries = currentEntries;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(BigDecimal entryFee) {
        this.entryFee = entryFee;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
    }

    public void setPrizePool(BigDecimal prizePool) {
        this.prizePool = prizePool;
    }

    public Integer getMaxWinners() {
        return maxWinners;
    }

    public void setMaxWinners(Integer maxWinners) {
        this.maxWinners = maxWinners;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
