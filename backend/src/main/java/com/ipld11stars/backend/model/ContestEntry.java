package com.ipld11stars.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A user's team in a contest. Status: ACTIVE, SETTLED (contest completed) or REFUNDED (contest cancelled).
 */
public class ContestEntry {

    private String id;
    private String contestId;
    private String userId;
    private String userHandle;
    private String entryName;
    private BigDecimal totalPoints = BigDecimal.ZERO;
    private Integer rank;
    private String status;
    private BigDecimal entryFeePaid = BigDecimal.ZERO;
    private BigDecimal prizeAmount = BigDecimal.ZERO;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserHandle() { return userHandle; }
    public void setUserHandle(String userHandle) { this.userHandle = userHandle; }
    public String getEntryName() { return entryName; }
    public void setEntryName(String entryName) { this.entryName = entryName; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal totalPoints) { this.totalPoints = totalPoints; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getEntryFeePaid() { return entryFeePaid; }
    public void setEntryFeePaid(BigDecimal entryFeePaid) { this.entryFeePaid = entryFeePaid; }
    public BigDecimal getPrizeAmount() { return prizeAmount; }
    public void setPrizeAmount(BigDecimal prizeAmount) { this.prizeAmount = prizeAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
