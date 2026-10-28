package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.PlayerMatchStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class PlayerMatchStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PlayerMatchStats> rowMapper = (rs, rowNum) -> {
        PlayerMatchStats s = new PlayerMatchStats(rs.getString("match_id"), rs.getString("player_id"));
        s.setRunsScored(rs.getInt("runs_scored"));
        s.setBallsFaced(rs.getInt("balls_faced"));
        s.setFours(rs.getInt("fours"));
        s.setSixes(rs.getInt("sixes"));
        s.setDismissed(rs.getBoolean("is_dismissed"));
        s.setWickets(rs.getInt("wickets"));
        s.setBallsBowled(rs.getInt("balls_bowled"));
        s.setMaidens(rs.getInt("maidens"));
        s.setRunsConceded(rs.getInt("runs_conceded"));
        s.setDotBalls(rs.getInt("dot_balls"));
        s.setLbwBowled(rs.getInt("lbw_bowled"));
        s.setCatches(rs.getInt("catches"));
        s.setStumpings(rs.getInt("stumpings"));
        s.setRunOuts(rs.getInt("run_outs"));
        s.setStrikeRate(rs.getBigDecimal("strike_rate"));
        s.setEconomyRate(rs.getBigDecimal("economy_rate"));
        s.setPoints(rs.getBigDecimal("points"));
        return s;
    };

    public PlayerMatchStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlayerMatchStats> findByMatchId(String matchId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.player_match_stats WHERE match_id = ? ORDER BY points DESC, player_id",
                rowMapper, matchId);
    }

    /** Replaces every stats row for the match with the freshly aggregated figures. */
    public void replaceForMatch(String matchId, Collection<PlayerMatchStats> stats) {
        jdbcTemplate.update("DELETE FROM ipld11stars.player_match_stats WHERE match_id = ?", matchId);
        String sql = "INSERT INTO ipld11stars.player_match_stats (id, match_id, player_id, runs_scored, balls_faced, fours, sixes, " +
                "is_dismissed, wickets, balls_bowled, overs_bowled, maidens, runs_conceded, dot_balls, lbw_bowled, catches, " +
                "stumpings, run_outs, strike_rate, economy_rate, points, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        for (PlayerMatchStats s : stats) {
            jdbcTemplate.update(sql, UUID.randomUUID().toString(), matchId, s.getPlayerId(), s.getRunsScored(),
                    s.getBallsFaced(), s.getFours(), s.getSixes(), s.isDismissed(), s.getWickets(), s.getBallsBowled(),
                    s.getOversBowled(), s.getMaidens(), s.getRunsConceded(), s.getDotBalls(), s.getLbwBowled(),
                    s.getCatches(), s.getStumpings(), s.getRunOuts(), s.getStrikeRate(), s.getEconomyRate(), s.getPoints());
        }
    }
}
