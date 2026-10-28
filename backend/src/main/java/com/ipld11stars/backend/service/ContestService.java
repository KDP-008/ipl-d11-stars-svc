package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.ContestDTO;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.repository.jdbc.ContestPrizeRepository;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContestService {

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ContestPrizeRepository prizeRepository;

    public List<ContestDTO> getAllContests() {
        List<Contest> contests = contestRepository.findAll();
        return contests.stream()
                .map(ContestDTO::new)
                .collect(Collectors.toList());
    }

    public Optional<Contest> getContestById(String id) {
        return contestRepository.findById(id);
    }

    public List<Contest> getContestsByUserId(String userId) {
        return contestRepository.findByCreatedByUserId(userId);
    }

    /** Prize table stored for the contest, or the default equal split if none was stored. */
    public List<ContestPrize> getPrizeTable(Contest contest) {
        List<ContestPrize> stored = prizeRepository.findByContestId(contest.getId());
        return stored.isEmpty() ? PrizeTable.defaultFor(contest.getPrizePool(), contest.getMaxWinners()) : stored;
    }

    public Contest createContest(String name, String matchId, String createdByUserId,
                                String contestType, Integer maxEntries, BigDecimal entryFee,
                                BigDecimal prizePool, Integer maxWinners) {
        return createContest(name, matchId, createdByUserId, contestType, maxEntries, entryFee, prizePool,
                maxWinners, null, null);
    }

    @Transactional
    public Contest createContest(String name, String matchId, String createdByUserId,
                                 String contestType, Integer maxEntries, BigDecimal entryFee,
                                 BigDecimal prizePool, Integer maxWinners, Integer minEntries,
                                 List<ContestPrize> prizes) {
        int min = minEntries != null ? minEntries : 2;
        validateMoneyRules(maxEntries, entryFee, prizePool, maxWinners, min);
        List<ContestPrize> table = prizes == null || prizes.isEmpty()
                ? PrizeTable.defaultFor(prizePool, maxWinners)
                : PrizeTable.validate(prizes, prizePool, maxWinners);

        Contest contest = new Contest(name, matchId, createdByUserId, contestType,
                maxEntries, entryFee, prizePool, maxWinners);
        contest.setMinEntries(min);
        Contest saved = contestRepository.save(contest);
        prizeRepository.replace(saved.getId(), table);
        return saved;
    }

    @Transactional
    public Contest updateContest(String contestId, String userId, String name, String contestType,
                                 Integer maxEntries, BigDecimal entryFee, BigDecimal prizePool,
                                 Integer maxWinners) {
        Optional<Contest> contestOpt = contestRepository.findById(contestId);
        if (contestOpt.isEmpty()) {
            throw new RuntimeException("Contest not found");
        }

        Contest contest = contestOpt.get();
        if (!contest.getCreatedByUserId().equals(userId)) {
            throw new RuntimeException("Only contest creator can edit the contest");
        }
        if (!"OPEN".equals(contest.getStatus())) {
            throw new RuntimeException("Only open contests can be edited");
        }
        boolean moneyChanged = entryFee != null || prizePool != null || maxWinners != null || maxEntries != null;
        if (moneyChanged && contest.getCurrentEntries() != null && contest.getCurrentEntries() > 0) {
            throw new RuntimeException("Entry fee, prize pool, winners and size cannot change once users have joined");
        }

        if (name != null) contest.setName(name);
        if (contestType != null) contest.setContestType(contestType);
        if (maxEntries != null) contest.setMaxEntries(maxEntries);
        if (entryFee != null) contest.setEntryFee(entryFee);
        if (prizePool != null) contest.setPrizePool(prizePool);
        if (maxWinners != null) contest.setMaxWinners(maxWinners);
        validateMoneyRules(contest.getMaxEntries(), contest.getEntryFee(), contest.getPrizePool(),
                contest.getMaxWinners(), contest.getMinEntries() != null ? contest.getMinEntries() : 2);

        Contest saved = contestRepository.save(contest);
        if (moneyChanged) {
            // A custom table no longer matches the new pool or winner count, so fall back to the default split.
            prizeRepository.replace(contestId, PrizeTable.defaultFor(saved.getPrizePool(), saved.getMaxWinners()));
        }
        return saved;
    }

    @Transactional
    public void deleteContest(String contestId, String userId) {
        if (!contestRepository.canUserEditContest(contestId, userId)) {
            throw new RuntimeException("Only contest creator can delete the contest");
        }
        Contest contest = contestRepository.findById(contestId).orElseThrow(() -> new RuntimeException("Contest not found"));
        if (contest.getCurrentEntries() != null && contest.getCurrentEntries() > 0) {
            throw new RuntimeException("A contest with entries cannot be deleted");
        }
        prizeRepository.deleteByContestId(contestId);
        contestRepository.delete(contestId, userId);
    }

    public boolean canUserEditContest(String contestId, String userId) {
        return contestRepository.canUserEditContest(contestId, userId);
    }

    private static void validateMoneyRules(Integer maxEntries, BigDecimal entryFee, BigDecimal prizePool,
                                           Integer maxWinners, int minEntries) {
        if (maxEntries == null || maxEntries < 2) {
            throw new BusinessRuleException("A contest needs room for at least 2 entries");
        }
        if (entryFee == null || entryFee.signum() < 0 || entryFee.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Entry fee must be zero or a positive amount with at most two decimal places");
        }
        if (prizePool == null || prizePool.signum() < 0 || prizePool.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Prize pool must be zero or a positive amount with at most two decimal places");
        }
        if (maxWinners == null || maxWinners < 1 || maxWinners > maxEntries) {
            throw new BusinessRuleException("Number of winners must be between 1 and the maximum number of entries");
        }
        if (minEntries < 2 || minEntries > maxEntries) {
            throw new BusinessRuleException("Minimum entries must be between 2 and the maximum number of entries");
        }
    }
}
