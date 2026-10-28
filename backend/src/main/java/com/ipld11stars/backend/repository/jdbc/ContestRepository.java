package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.Contest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContestRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Contest> contestRowMapper = (ResultSet rs, int rowNum) -> {
        Contest contest = new Contest();
        contest.setId(rs.getString("id"));
        contest.setName(rs.getString("name"));
        contest.setMatchId(rs.getString("match_id"));
        contest.setCreatedByUserId(rs.getString("created_by_user_id"));
        contest.setContestType(rs.getString("contest_type"));
        contest.setVisibility(rs.getString("visibility"));
        contest.setMaxEntries(rs.getInt("max_entries"));
        contest.setCurrentEntries(rs.getInt("current_entries"));
        contest.setEntryFee(rs.getBigDecimal("entry_fee"));
        contest.setPrizePool(rs.getBigDecimal("prize_pool"));
        contest.setMaxWinners(rs.getInt("max_winners"));
        contest.setMinEntries(rs.getInt("min_entries"));
        contest.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            contest.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            contest.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return contest;
    };

    public Optional<Contest> findById(String id) {
        String sql = "SELECT * FROM ipld11stars.contests WHERE id = ?";
        try {
            Contest contest = jdbcTemplate.queryForObject(sql, contestRowMapper, id);
            return Optional.of(contest);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Contest> findByMatchId(String matchId) {
        String sql = "SELECT * FROM ipld11stars.contests WHERE match_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, contestRowMapper, matchId);
    }

    public List<Contest> findByCreatedByUserId(String userId) {
        String sql = "SELECT * FROM ipld11stars.contests WHERE created_by_user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, contestRowMapper, userId);
    }

    public List<Contest> findAll() {
        String sql = "SELECT * FROM ipld11stars.contests ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, contestRowMapper);
    }

    public Contest save(Contest contest) {
        if (contest.getId() == null) {
            return insert(contest);
        } else {
            return update(contest);
        }
    }

    private Contest insert(Contest contest) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO ipld11stars.contests (id, name, match_id, created_by_user_id, contest_type, " +
                    "visibility, max_entries, current_entries, entry_fee, prize_pool, max_winners, min_entries, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, id, contest.getName(), contest.getMatchId(), contest.getCreatedByUserId(),
                           contest.getContestType(), contest.getVisibility(), contest.getMaxEntries(),
                           contest.getCurrentEntries(), contest.getEntryFee(), contest.getPrizePool(),
                           contest.getMaxWinners(), contest.getMinEntries() != null ? contest.getMinEntries() : 2,
                           contest.getStatus());
        contest.setId(id);
        return contest;
    }

    private Contest update(Contest contest) {
        String sql = "UPDATE ipld11stars.contests SET name = ?, match_id = ?, contest_type = ?, " +
                    "visibility = ?, max_entries = ?, current_entries = ?, entry_fee = ?, prize_pool = ?, " +
                    "max_winners = ?, status = ?, updated_at = NOW() WHERE id = ? AND created_by_user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, contest.getName(), contest.getMatchId(),
                           contest.getContestType(), contest.getVisibility(), contest.getMaxEntries(),
                           contest.getCurrentEntries(), contest.getEntryFee(), contest.getPrizePool(),
                           contest.getMaxWinners(), contest.getStatus(), contest.getId(),
                           contest.getCreatedByUserId());
        if (rowsAffected == 0) {
            throw new RuntimeException("Contest not found or user is not the creator");
        }
        return contest;
    }

    public void delete(String id, String createdByUserId) {
        String sql = "DELETE FROM ipld11stars.contests WHERE id = ? AND created_by_user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id, createdByUserId);
        if (rowsAffected == 0) {
            throw new RuntimeException("Contest not found or user is not the creator");
        }
    }

    /** Takes one entry slot if the contest is open and not full; returns false otherwise. */
    public boolean tryTakeEntrySlot(String contestId) {
        return jdbcTemplate.update("UPDATE ipld11stars.contests SET current_entries = current_entries + 1, updated_at = NOW() " +
                "WHERE id = ? AND status = 'OPEN' AND current_entries < max_entries", contestId) == 1;
    }

    /** Moves a contest from one of the expected statuses to a new one; returns false if it was in another status. */
    public boolean transitionStatus(String contestId, List<String> fromStatuses, String toStatus) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(fromStatuses.size(), "?"));
        String timestampColumn = switch (toStatus) {
            case "LOCKED" -> ", locked_at = NOW()";
            case "COMPLETED", "CANCELLED" -> ", settled_at = NOW()";
            default -> "";
        };
        Object[] args = new Object[fromStatuses.size() + 2];
        args[0] = toStatus;
        args[1] = contestId;
        for (int i = 0; i < fromStatuses.size(); i++) {
            args[i + 2] = fromStatuses.get(i);
        }
        return jdbcTemplate.update("UPDATE ipld11stars.contests SET status = ?, updated_at = NOW()" + timestampColumn +
                " WHERE id = ? AND status IN (" + placeholders + ")", args) == 1;
    }

    public List<Contest> findByMatchIdAndStatusIn(String matchId, List<String> statuses) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(statuses.size(), "?"));
        Object[] args = new Object[statuses.size() + 1];
        args[0] = matchId;
        for (int i = 0; i < statuses.size(); i++) {
            args[i + 1] = statuses.get(i);
        }
        return jdbcTemplate.query("SELECT * FROM ipld11stars.contests WHERE match_id = ? AND status IN (" + placeholders +
                ") ORDER BY created_at, id", contestRowMapper, args);
    }

    public boolean canUserEditContest(String contestId, String userId) {
        String sql = "SELECT COUNT(*) FROM ipld11stars.contests WHERE id = ? AND created_by_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, contestId, userId);
        return count != null && count > 0;
    }
}
