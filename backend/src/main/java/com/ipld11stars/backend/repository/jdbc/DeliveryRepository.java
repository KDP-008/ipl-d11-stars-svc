package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.Delivery;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class DeliveryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Delivery> rowMapper = (rs, rowNum) -> {
        String dismissal = rs.getString("dismissal_type");
        Timestamp created = rs.getTimestamp("created_at");
        return new Delivery(
                rs.getString("id"),
                rs.getString("match_id"),
                rs.getInt("innings"),
                rs.getInt("sequence_no"),
                rs.getInt("over_number"),
                rs.getInt("ball_number"),
                rs.getString("batting_team_id"),
                rs.getString("bowling_team_id"),
                rs.getString("striker_id"),
                rs.getString("non_striker_id"),
                rs.getString("bowler_id"),
                rs.getInt("runs_batter"),
                ExtrasType.valueOf(rs.getString("extras_type")),
                rs.getInt("extras_runs"),
                rs.getBoolean("is_legal"),
                rs.getBoolean("is_free_hit"),
                rs.getBoolean("is_wicket"),
                dismissal != null ? DismissalType.valueOf(dismissal) : null,
                rs.getString("dismissed_player_id"),
                rs.getString("fielder_id"),
                created != null ? created.toLocalDateTime() : null);
    };

    public DeliveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Delivery> findByMatchId(String matchId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.match_deliveries WHERE match_id = ? ORDER BY innings, sequence_no",
                rowMapper, matchId);
    }

    public void insert(Delivery d) {
        String sql = "INSERT INTO ipld11stars.match_deliveries (id, match_id, innings, sequence_no, over_number, ball_number, " +
                "batting_team_id, bowling_team_id, striker_id, non_striker_id, bowler_id, runs_batter, extras_type, extras_runs, " +
                "is_legal, is_free_hit, is_wicket, dismissal_type, dismissed_player_id, fielder_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, d.id(), d.matchId(), d.innings(), d.sequenceNo(), d.overNumber(), d.ballNumber(),
                d.battingTeamId(), d.bowlingTeamId(), d.strikerId(), d.nonStrikerId(), d.bowlerId(), d.runsBatter(),
                d.extrasType().name(), d.extrasRuns(), d.legal(), d.freeHit(), d.wicket(),
                d.dismissalType() != null ? d.dismissalType().name() : null, d.dismissedPlayerId(), d.fielderId());
    }
}
