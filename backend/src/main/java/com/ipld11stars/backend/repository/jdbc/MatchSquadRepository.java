package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.PlayerRole;
import com.ipld11stars.backend.model.SquadMember;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MatchSquadRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SquadMember> rowMapper = (rs, rowNum) -> new SquadMember(
            rs.getString("match_id"),
            rs.getString("player_id"),
            rs.getString("team_id"),
            rs.getString("full_name"),
            PlayerRole.fromCode(rs.getString("role")),
            rs.getBigDecimal("credits"),
            rs.getBoolean("is_starting_xi"));

    public MatchSquadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SquadMember> findByMatchId(String matchId) {
        String sql = "SELECT s.match_id, s.player_id, s.team_id, s.is_starting_xi, p.full_name, p.role, p.credits " +
                "FROM ipld11stars.match_squad s JOIN ipld11stars.players p ON p.id = s.player_id " +
                "WHERE s.match_id = ? ORDER BY s.team_id, s.player_id";
        return jdbcTemplate.query(sql, rowMapper, matchId);
    }

    public void deleteByMatchAndTeam(String matchId, String teamId) {
        jdbcTemplate.update("DELETE FROM ipld11stars.match_squad WHERE match_id = ? AND team_id = ?", matchId, teamId);
    }

    public void insert(String matchId, String playerId, String teamId, boolean startingXi) {
        jdbcTemplate.update("INSERT INTO ipld11stars.match_squad (id, match_id, player_id, team_id, is_starting_xi, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())",
                UUID.randomUUID().toString(), matchId, playerId, teamId, startingXi);
    }
}
