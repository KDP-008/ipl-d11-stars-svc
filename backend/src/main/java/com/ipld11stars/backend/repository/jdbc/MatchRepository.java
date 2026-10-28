package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.Match;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MatchRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Match> matchRowMapper = (ResultSet rs, int rowNum) -> {
        Match match = new Match();
        match.setId(rs.getString("id"));
        match.setMatchNumber(rs.getInt("match_number"));
        match.setHomeTeamId(rs.getString("team_home_id"));
        match.setAwayTeamId(rs.getString("team_away_id"));
        match.setSeasonId(rs.getString("season_id"));
        match.setVenue(rs.getString("venue"));
        Timestamp startTime = rs.getTimestamp("start_time_utc");
        if (startTime != null) {
            match.setStartTimeUtc(startTime.toLocalDateTime());
        }
        match.setMatchStatus(rs.getString("status"));
        match.setMatchType(rs.getString("match_type"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            match.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            match.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return match;
    };

    public Optional<Match> findById(String id) {
        String sql = "SELECT * FROM ipld11stars.matches WHERE id = ?";
        try {
            Match match = jdbcTemplate.queryForObject(sql, matchRowMapper, id);
            return Optional.of(match);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Match> findBySeasonId(String seasonId) {
        String sql = "SELECT * FROM ipld11stars.matches WHERE season_id = ? ORDER BY match_number";
        return jdbcTemplate.query(sql, matchRowMapper, seasonId);
    }

    public List<Match> findAll() {
        String sql = "SELECT * FROM ipld11stars.matches ORDER BY start_time_utc DESC";
        return jdbcTemplate.query(sql, matchRowMapper);
    }

    public List<Match> findUpcoming() {
        String sql = "SELECT * FROM ipld11stars.matches WHERE start_time_utc > NOW() AND status = 'UPCOMING' " +
                    "ORDER BY start_time_utc ASC";
        return jdbcTemplate.query(sql, matchRowMapper);
    }

    public Optional<Match> findByMatchNumberAndSeason(Integer matchNumber, String seasonId) {
        String sql = "SELECT * FROM ipld11stars.matches WHERE match_number = ? AND season_id = ?";
        try {
            Match match = jdbcTemplate.queryForObject(sql, matchRowMapper, matchNumber, seasonId);
            return Optional.of(match);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Match save(Match match) {
        if (match.getId() == null) {
            return insert(match);
        } else {
            return update(match);
        }
    }

    private Match insert(Match match) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO ipld11stars.matches (id, match_number, team_home_id, team_away_id, season_id, " +
                    "venue, start_time_utc, status, match_type, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, id, match.getMatchNumber(), match.getHomeTeamId(), match.getAwayTeamId(),
                           match.getSeasonId(), match.getVenue(), match.getStartTimeUtc(),
                           match.getMatchStatus(), match.getMatchType());
        match.setId(id);
        return match;
    }

    private Match update(Match match) {
        String sql = "UPDATE ipld11stars.matches SET match_number = ?, team_home_id = ?, team_away_id = ?, " +
                    "season_id = ?, venue = ?, start_time_utc = ?, status = ?, match_type = ?, updated_at = NOW() " +
                    "WHERE id = ?";
        jdbcTemplate.update(sql, match.getMatchNumber(), match.getHomeTeamId(), match.getAwayTeamId(),
                           match.getSeasonId(), match.getVenue(), match.getStartTimeUtc(),
                           match.getMatchStatus(), match.getMatchType(), match.getId());
        return match;
    }

    public void delete(String id) {
        String sql = "DELETE FROM ipld11stars.matches WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
