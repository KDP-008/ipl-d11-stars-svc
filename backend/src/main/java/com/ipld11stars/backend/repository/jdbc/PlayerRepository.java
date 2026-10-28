package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.Player;
import com.ipld11stars.backend.model.PlayerRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Player> rowMapper = (rs, rowNum) -> new Player(
            rs.getString("id"),
            rs.getString("team_id"),
            rs.getString("full_name"),
            PlayerRole.fromCode(rs.getString("role")),
            rs.getBigDecimal("credits"));

    public PlayerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Player> findById(String id) {
        List<Player> rows = jdbcTemplate.query("SELECT * FROM ipld11stars.players WHERE id = ?", rowMapper, id);
        return rows.stream().findFirst();
    }

    public List<Player> findByTeamId(String teamId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.players WHERE team_id = ? ORDER BY id", rowMapper, teamId);
    }

    public void insert(Player player) {
        jdbcTemplate.update("INSERT INTO ipld11stars.players (id, team_id, full_name, role, credits, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                player.id(), player.teamId(), player.fullName(), player.role().name(),
                player.credits() != null ? player.credits() : BigDecimal.valueOf(8.0));
    }
}
