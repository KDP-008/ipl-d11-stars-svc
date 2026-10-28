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
                    "visibility, max_entries, current_entries, entry_fee, prize_pool, max_winners, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, id, contest.getName(), contest.getMatchId(), contest.getCreatedByUserId(),
                           contest.getContestType(), contest.getVisibility(), contest.getMaxEntries(),
                           contest.getCurrentEntries(), contest.getEntryFee(), contest.getPrizePool(),
                           contest.getMaxWinners(), contest.getStatus());
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

    public boolean canUserEditContest(String contestId, String userId) {
        String sql = "SELECT COUNT(*) FROM ipld11stars.contests WHERE id = ? AND created_by_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, contestId, userId);
        return count != null && count > 0;
    }
}
