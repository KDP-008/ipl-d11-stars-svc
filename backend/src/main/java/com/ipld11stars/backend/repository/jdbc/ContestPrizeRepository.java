package com.ipld11stars.backend.repository.jdbc;

import com.ipld11stars.backend.model.ContestPrize;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ContestPrizeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ContestPrizeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ContestPrize> findByContestId(String contestId) {
        return jdbcTemplate.query("SELECT rank_from, rank_to, amount FROM ipld11stars.contest_prizes WHERE contest_id = ? ORDER BY rank_from",
                (rs, n) -> new ContestPrize(rs.getInt("rank_from"), rs.getInt("rank_to"), rs.getBigDecimal("amount")), contestId);
    }

    public void replace(String contestId, List<ContestPrize> prizes) {
        deleteByContestId(contestId);
        for (ContestPrize p : prizes) {
            jdbcTemplate.update("INSERT INTO ipld11stars.contest_prizes (id, contest_id, rank_from, rank_to, amount, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW())", UUID.randomUUID().toString(), contestId, p.rankFrom(), p.rankTo(), p.amount());
        }
    }

    public void deleteByContestId(String contestId) {
        jdbcTemplate.update("DELETE FROM ipld11stars.contest_prizes WHERE contest_id = ?", contestId);
    }
}
