package com.ipld11stars.backend.support;

import com.ipld11stars.backend.dto.JoinContestRequest;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.service.ContestService;
import com.ipld11stars.backend.service.WalletService;
import com.ipld11stars.backend.support.MatchFixtures.TestMatch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Contest, wallet and team helpers on top of {@link MatchFixtures}.
 */
@Component
public class ContestFixtures {

    private final ContestService contestService;
    private final WalletService walletService;
    private final UserFixtures users;

    public ContestFixtures(ContestService contestService, WalletService walletService, UserFixtures users) {
        this.contestService = contestService;
        this.walletService = walletService;
        this.users = users;
    }

    public Contest paidContest(TestMatch m, String entryFee, String prizePool, int maxEntries, int maxWinners,
                               List<ContestPrize> prizes) {
        User creator = users.create("Host");
        return contestService.createContest("League " + UUID.randomUUID().toString().substring(0, 6), m.matchId(),
                creator.getId(), "PAID", maxEntries, new BigDecimal(entryFee), new BigDecimal(prizePool), maxWinners,
                2, prizes);
    }

    /** A user with money in their wallet. */
    public User fundedUser(String name, String amount) {
        User u = users.create(name);
        walletService.credit(u.getId(), new BigDecimal(amount), TransactionType.DEPOSIT, "TEST", null,
                "seed:" + u.getId(), "test funds");
        return u;
    }

    /**
     * A valid team: home wk1, bat1-3, ar1, bowl1-2 (7 players) + away bat1, ar1, bowl1-2 (4 players).
     * Roles: WK 1, BAT 4, AR 2, BOWL 4. Credits: 7 x 9.0 + 4 x 8.5 = 97.0.
     */
    public static List<String> validTeam(TestMatch m) {
        List<String> ids = new ArrayList<>(List.of(
                m.home("wk1"), m.home("bat1"), m.home("bat2"), m.home("bat3"), m.home("ar1"),
                m.home("bowl1"), m.home("bowl2"),
                m.away("bat1"), m.away("ar1"), m.away("bowl1"), m.away("bowl2")));
        return ids;
    }

    public static JoinContestRequest team(TestMatch m, String captain, String viceCaptain) {
        return new JoinContestRequest("My XI", validTeam(m), captain, viceCaptain);
    }
}
