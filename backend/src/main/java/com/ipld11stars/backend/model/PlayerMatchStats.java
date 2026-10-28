package com.ipld11stars.backend.model;

import java.math.BigDecimal;

/**
 * Aggregated per-match figures for one player plus the fantasy points they earned.
 */
public class PlayerMatchStats {

    private String matchId;
    private String playerId;
    private int runsScored;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private boolean dismissed;
    private int wickets;
    private int ballsBowled;
    private int maidens;
    private int runsConceded;
    private int dotBalls;
    private int lbwBowled;
    private int catches;
    private int stumpings;
    private int runOuts;
    private BigDecimal strikeRate;
    private BigDecimal economyRate;
    private BigDecimal points = BigDecimal.ZERO;

    public PlayerMatchStats() {
    }

    public PlayerMatchStats(String matchId, String playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
    }

    /** Overs in cricket notation, e.g. 3.4 for 22 legal balls. */
    public BigDecimal getOversBowled() {
        return new BigDecimal((ballsBowled / 6) + "." + (ballsBowled % 6));
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getRunsScored() { return runsScored; }
    public void setRunsScored(int runsScored) { this.runsScored = runsScored; }
    public int getBallsFaced() { return ballsFaced; }
    public void setBallsFaced(int ballsFaced) { this.ballsFaced = ballsFaced; }
    public int getFours() { return fours; }
    public void setFours(int fours) { this.fours = fours; }
    public int getSixes() { return sixes; }
    public void setSixes(int sixes) { this.sixes = sixes; }
    public boolean isDismissed() { return dismissed; }
    public void setDismissed(boolean dismissed) { this.dismissed = dismissed; }
    public int getWickets() { return wickets; }
    public void setWickets(int wickets) { this.wickets = wickets; }
    public int getBallsBowled() { return ballsBowled; }
    public void setBallsBowled(int ballsBowled) { this.ballsBowled = ballsBowled; }
    public int getMaidens() { return maidens; }
    public void setMaidens(int maidens) { this.maidens = maidens; }
    public int getRunsConceded() { return runsConceded; }
    public void setRunsConceded(int runsConceded) { this.runsConceded = runsConceded; }
    public int getDotBalls() { return dotBalls; }
    public void setDotBalls(int dotBalls) { this.dotBalls = dotBalls; }
    public int getLbwBowled() { return lbwBowled; }
    public void setLbwBowled(int lbwBowled) { this.lbwBowled = lbwBowled; }
    public int getCatches() { return catches; }
    public void setCatches(int catches) { this.catches = catches; }
    public int getStumpings() { return stumpings; }
    public void setStumpings(int stumpings) { this.stumpings = stumpings; }
    public int getRunOuts() { return runOuts; }
    public void setRunOuts(int runOuts) { this.runOuts = runOuts; }
    public BigDecimal getStrikeRate() { return strikeRate; }
    public void setStrikeRate(BigDecimal strikeRate) { this.strikeRate = strikeRate; }
    public BigDecimal getEconomyRate() { return economyRate; }
    public void setEconomyRate(BigDecimal economyRate) { this.economyRate = economyRate; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
}
