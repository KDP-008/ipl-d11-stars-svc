package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.ContestDTO;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContestService {

    @Autowired
    private ContestRepository contestRepository;

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

    public Contest createContest(String name, String matchId, String createdByUserId,
                                String contestType, Integer maxEntries, BigDecimal entryFee,
                                BigDecimal prizePool, Integer maxWinners) {
        Contest contest = new Contest(name, matchId, createdByUserId, contestType,
                maxEntries, entryFee, prizePool, maxWinners);
        return contestRepository.save(contest);
    }

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

        if (name != null) contest.setName(name);
        if (contestType != null) contest.setContestType(contestType);
        if (maxEntries != null) contest.setMaxEntries(maxEntries);
        if (entryFee != null) contest.setEntryFee(entryFee);
        if (prizePool != null) contest.setPrizePool(prizePool);
        if (maxWinners != null) contest.setMaxWinners(maxWinners);

        return contestRepository.save(contest);
    }

    public void deleteContest(String contestId, String userId) {
        if (!contestRepository.canUserEditContest(contestId, userId)) {
            throw new RuntimeException("Only contest creator can delete the contest");
        }
        contestRepository.delete(contestId, userId);
    }

    public boolean canUserEditContest(String contestId, String userId) {
        return contestRepository.canUserEditContest(contestId, userId);
    }
}