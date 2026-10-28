package com.ipld11stars.backend.contest;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.repository.jdbc.ContestEntryRepository;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.service.ContestEntryService;
import com.ipld11stars.backend.service.ContestLifecycleService;
import com.ipld11stars.backend.service.DeliveryService;
import com.ipld11stars.backend.service.WalletService;
import com.ipld11stars.backend.support.ContestFixtures;
import com.ipld11stars.backend.support.MatchFixtures;
import com.ipld11stars.backend.support.MatchFixtures.TestMatch;
import com.ipld11stars.backend.support.MutableClock;
import com.ipld11stars.backend.support.RecordingEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ipld11stars.backend.support.ContestFixtures.team;
import static com.ipld11stars.backend.support.MatchFixtures.ball;
import static com.ipld11stars.backend.support.MatchFixtures.wicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContestLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired private MatchFixtures matches;
    @Autowired private ContestFixtures contests;
    @Autowired private ContestEntryService entryService;
    @Autowired private ContestLifecycleService lifecycle;
    @Autowired private DeliveryService deliveries;
    @Autowired private ContestRepository contestRepository;
    @Autowired private ContestEntryRepository entryRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private WalletService walletService;
    @Autowired private RecordingEmailSender emails;
    @Autowired private MutableClock clock;

    private TestMatch m;

    @BeforeEach
    void setUp() {
        clock.reset();
        m = matches.createMatch(clock.nowUtc().plusHours(1));
    }

    private String contestStatus(Contest c) {
        return contestRepository.findById(c.getId()).orElseThrow().getStatus();
    }

    private Map<String, ContestEntry> entriesByUser(Contest c) {
        return entryRepository.findByContestId(c.getId()).stream()
                .collect(Collectors.toMap(ContestEntry::getUserId, Function.identity()));
    }

    /**
     * Home side: bat1 hits a six off the first ball, then ten wickets fall (bowled) in overs 1-2
     * (away bowl1 takes five, away bowl2 five) -> 6 all out. Away side chases 7: bat1 hits 6 then 1.
     */
    private void playShortMatch() {
        String h = m.home();
        String[] order = {"wk1", "bat1", "bat2", "bat3", "bat4", "ar1", "ar2", "bowl1", "bowl2", "bowl3", "bowl4"};
        deliveries.record(m.matchId(), ball(1, 1, h, m.home("bat1"), m.home("wk1"), m.away("bowl1"), 6));
        for (int i = 1; i <= 10; i++) {
            int over = i <= 5 ? 1 : 2;
            String bowler = over == 1 ? m.away("bowl1") : m.away("bowl2");
            String striker = m.home(order[i]);
            deliveries.record(m.matchId(), wicket(1, over, h, striker, m.home("wk1"), bowler, DismissalType.BOWLED, striker, null));
        }
        deliveries.record(m.matchId(), ball(2, 1, m.away(), m.away("bat1"), m.away("bat2"), m.home("bowl1"), 6));
        deliveries.record(m.matchId(), ball(2, 1, m.away(), m.away("bat1"), m.away("bat2"), m.home("bowl1"), 1));
    }

    @Test
    void underFilledContestIsCancelledAndRefundedAtMatchStart() {
        Contest contest = contests.paidContest(m, "25.00", "40.00", 10, 1, null);
        User u = contests.fundedUser("Solo", "100");
        entryService.join(u.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1")));
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("75.00");

        assertThat(lifecycle.lockDueContests()).as("match has not started yet").isZero();
        clock.advance(Duration.ofMinutes(61));
        lifecycle.lockDueContests();

        assertThat(contestStatus(contest)).isEqualTo("CANCELLED");
        assertThat(entriesByUser(contest).get(u.getId()).getStatus()).isEqualTo("REFUNDED");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("100.00");
        assertThat(emails.sentTo(u.getEmail())).anyMatch(e -> e.subject().contains("was cancelled"));

        // running the lock again does not refund twice
        lifecycle.lockDueContests();
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("100.00");
    }

    @Test
    void liveLeaderboardThenSettlementPaysPrizesByPosition() throws Exception {
        Contest contest = contests.paidContest(m, "100.00", "300.00", 3, 2,
                List.of(new ContestPrize(1, 1, new BigDecimal("200")), new ContestPrize(2, 2, new BigDecimal("100"))));
        User a = contests.fundedUser("Anil", "1000");
        User b = contests.fundedUser("Bina", "1000");
        User c = contests.fundedUser("Chetan", "1000");
        entryService.join(a.getId(), contest.getId(), team(m, m.away("bowl1"), m.away("bowl2")));
        clock.advance(Duration.ofMinutes(1));
        entryService.join(b.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bat1")));
        clock.advance(Duration.ofMinutes(1));
        entryService.join(c.getId(), contest.getId(), team(m, m.away("bowl1"), m.away("bowl2")));

        assertThatThrownBy(() -> lifecycle.completeMatch(m.matchId())).hasMessageContaining("live match");
        playShortMatch();
        assertThat(contestStatus(contest)).isEqualTo("LIVE");
        assertThatThrownBy(() -> entryService.join(contests.fundedUser("Late", "500").getId(), contest.getId(),
                team(m, m.home("bat1"), m.away("bowl1")))).hasMessageContaining("no longer accepting");

        Map<String, ContestEntry> live = entriesByUser(contest);
        // base 461 points; A and C double away bowl1 (203) and add half of away bowl2 (203)
        assertThat(live.get(a.getId()).getTotalPoints()).isEqualByComparingTo("765.50");
        assertThat(live.get(c.getId()).getTotalPoints()).isEqualByComparingTo("765.50");
        assertThat(live.get(b.getId()).getTotalPoints()).isEqualByComparingTo("485.50");
        assertThat(live.get(a.getId()).getRank()).isEqualTo(1);
        assertThat(live.get(c.getId()).getRank()).as("tied totals share a rank").isEqualTo(1);
        assertThat(live.get(b.getId()).getRank()).isEqualTo(3);

        String admin = obtainAccessToken("contestadmin", "admin123");
        mockMvc.perform(post("/matches/" + m.matchId() + "/complete").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        assertThat(contestStatus(contest)).isEqualTo("COMPLETED");
        assertThat(matchRepository.findById(m.matchId()).orElseThrow().getMatchStatus()).isEqualTo("COMPLETED");
        Map<String, ContestEntry> settled = entriesByUser(contest);
        // tied on points: the earlier entry (A) takes position 1 and its prize
        assertThat(settled.get(a.getId()).getPrizeAmount()).isEqualByComparingTo("200.00");
        assertThat(settled.get(c.getId()).getPrizeAmount()).isEqualByComparingTo("100.00");
        assertThat(settled.get(b.getId()).getPrizeAmount()).isEqualByComparingTo("0");
        assertThat(walletService.getBalance(a.getId())).isEqualByComparingTo("1100.00");
        assertThat(walletService.getBalance(c.getId())).isEqualByComparingTo("1000.00");
        assertThat(walletService.getBalance(b.getId())).isEqualByComparingTo("900.00");
        assertThat(emails.sentTo(a.getEmail())).anyMatch(e -> e.body().contains("Winnings: Rs 200.00"));

        mockMvc.perform(get("/contests/" + contest.getId() + "/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(a.getId()))
                .andExpect(jsonPath("$[0].userHandle").value(a.getUserId()))
                .andExpect(jsonPath("$[2].userId").value(b.getId()));

        assertThatThrownBy(() -> lifecycle.completeMatch(m.matchId())).hasMessageContaining("live match");
        assertThat(walletService.getBalance(a.getId())).isEqualByComparingTo("1100.00");
    }

    @Test
    void cannotCompleteBeforeTheResultAndAbandonRefundsEveryone() {
        Contest contest = contests.paidContest(m, "50.00", "80.00", 5, 1, null);
        User a = contests.fundedUser("Om", "100");
        User b = contests.fundedUser("Pia", "100");
        entryService.join(a.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1")));
        entryService.join(b.getId(), contest.getId(), team(m, m.home("bat2"), m.away("bowl2")));

        deliveries.record(m.matchId(), ball(1, 1, m.home(), m.home("bat1"), m.home("bat2"), m.away("bowl1"), 1));
        assertThat(contestStatus(contest)).isEqualTo("LIVE");
        assertThatThrownBy(() -> lifecycle.completeMatch(m.matchId())).hasMessageContaining("not finished");

        lifecycle.abandonMatch(m.matchId());
        assertThat(contestStatus(contest)).isEqualTo("CANCELLED");
        assertThat(walletService.getBalance(a.getId())).isEqualByComparingTo("100.00");
        assertThat(walletService.getBalance(b.getId())).isEqualByComparingTo("100.00");
        assertThat(matchRepository.findById(m.matchId()).orElseThrow().getMatchStatus()).isEqualTo("ABANDONED");
    }

    @Test
    void matchControlEndpointsAreAdminOnly() throws Exception {
        String player = obtainAccessToken("testplayer", "password123");
        mockMvc.perform(post("/matches/" + m.matchId() + "/abandon").header("Authorization", "Bearer " + player)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
