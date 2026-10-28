package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.EntrySelection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContestEntryRepository {

    private static final String SELECT = "SELECT e.*, u.user_id AS user_handle FROM ipld11stars.contest_entries e " +
            "LEFT JOIN ipld11stars.users u ON u.id = e.user_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ContestEntry> rowMapper = (rs, rowNum) -> {
        ContestEntry e = new ContestEntry();
        e.setId(rs.getString("id"));
        e.setContestId(rs.getString("contest_id"));
        e.setUserId(rs.getString("user_id"));
        e.setUserHandle(rs.getString("user_handle"));
        e.setEntryName(rs.getString("entry_name"));
        e.setTotalPoints(rs.getBigDecimal("total_points"));
        int rank = rs.getInt("rank");
        e.setRank(rs.wasNull() ? null : rank);
        e.setStatus(rs.getString("status"));
        e.setEntryFeePaid(rs.getBigDecimal("entry_fee_paid"));
        e.setPrizeAmount(rs.getBigDecimal("prize_amount"));
        Timestamp created = rs.getTimestamp("created_at");
        e.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        return e;
    };

    private final RowMapper<EntrySelection> selectionMapper = (rs, rowNum) -> new EntrySelection(
            rs.getString("contest_entry_id"), rs.getString("player_id"),
            rs.getBoolean("is_captain"), rs.getBoolean("is_vice_captain"));

    public ContestEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ContestEntry e) {
        jdbcTemplate.update("INSERT INTO ipld11stars.contest_entries (id, contest_id, user_id, entry_name, total_points, status, " +
                        "entry_fee_paid, prize_amount, created_at, updated_at) VALUES (?, ?, ?, ?, 0.00, 'ACTIVE', ?, 0.00, ?, NOW())",
                e.getId(), e.getContestId(), e.getUserId(), e.getEntryName(), e.getEntryFeePaid(),
                Timestamp.valueOf(e.getCreatedAt()));
    }

    public void insertSelections(String entryId, List<EntrySelection> selections) {
        for (EntrySelection s : selections) {
            jdbcTemplate.update("INSERT INTO ipld11stars.entry_player_selections (id, contest_entry_id, player_id, is_captain, " +
                            "is_vice_captain, selected_at) VALUES (?, ?, ?, ?, ?, NOW())",
                    UUID.randomUUID().toString(), entryId, s.playerId(), s.captain(), s.viceCaptain());
        }
    }

    public Optional<ContestEntry> findByContestAndUser(String contestId, String userId) {
        return jdbcTemplate.query(SELECT + "WHERE e.contest_id = ? AND e.user_id = ?", rowMapper, contestId, userId)
                .stream().findFirst();
    }

    /** Entries in leaderboard order: points high to low, then earliest entry, then id. */
    public List<ContestEntry> findByContestId(String contestId) {
        return jdbcTemplate.query(SELECT + "WHERE e.contest_id = ? ORDER BY e.total_points DESC, e.created_at, e.id",
                rowMapper, contestId);
    }

    public List<EntrySelection> findSelectionsByContestId(String contestId) {
        return jdbcTemplate.query("SELECT s.* FROM ipld11stars.entry_player_selections s " +
                "JOIN ipld11stars.contest_entries e ON e.id = s.contest_entry_id WHERE e.contest_id = ?", selectionMapper, contestId);
    }

    public List<EntrySelection> findSelections(String entryId) {
        return jdbcTemplate.query("SELECT * FROM ipld11stars.entry_player_selections WHERE contest_entry_id = ? ORDER BY player_id",
                selectionMapper, entryId);
    }

    public void updatePointsAndRank(String entryId, BigDecimal points, int rank) {
        jdbcTemplate.update("UPDATE ipld11stars.contest_entries SET total_points = ?, rank = ?, updated_at = NOW() WHERE id = ?",
                points, rank, entryId);
    }

    public void markSettled(String entryId, BigDecimal prize) {
        jdbcTemplate.update("UPDATE ipld11stars.contest_entries SET status = 'SETTLED', prize_amount = ?, updated_at = NOW() WHERE id = ?",
                prize, entryId);
    }

    public void markRefunded(String entryId) {
        jdbcTemplate.update("UPDATE ipld11stars.contest_entries SET status = 'REFUNDED', updated_at = NOW() WHERE id = ?", entryId);
    }
}
