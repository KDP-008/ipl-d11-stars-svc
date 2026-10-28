package com.ipld11stars.backend.contest;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.dto.JoinContestRequest;
import com.ipld11stars.backend.dto.SquadRequest;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.Player;
import com.ipld11stars.backend.model.PlayerRole;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.PlayerRepository;
import com.ipld11stars.backend.service.ContestEntryService;
import com.ipld11stars.backend.service.MatchSquadService;
import com.ipld11stars.backend.service.WalletService;
import com.ipld11stars.backend.support.ContestFixtures;
import com.ipld11stars.backend.support.MatchFixtures;
import com.ipld11stars.backend.support.MatchFixtures.TestMatch;
import com.ipld11stars.backend.support.MutableClock;
import com.ipld11stars.backend.support.RecordingEmailSender;
import com.ipld11stars.backend.support.UserFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static com.ipld11stars.backend.support.ContestFixtures.team;
import static com.ipld11stars.backend.support.ContestFixtures.validTeam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContestEntryIntegrationTest extends BaseIntegrationTest {

    @Autowired private MatchFixtures matches;
    @Autowired private ContestFixtures contests;
    @Autowired private ContestEntryService entryService;
    @Autowired private ContestRepository contestRepository;
    @Autowired private WalletService walletService;
    @Autowired private MatchSquadService squadService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private RecordingEmailSender emails;
    @Autowired private MutableClock clock;

    private TestMatch m;
    private Contest contest;

    @BeforeEach
    void setUp() {
        clock.reset();
        m = matches.createMatch(clock.nowUtc().plusHours(1));
        contest = contests.paidContest(m, "49.50", "90.00", 2, 1, null);
    }

    private JoinContestRequest withTeam(UnaryOperator<List<String>> change, String c, String vc) {
        return new JoinContestRequest("XI", change.apply(new ArrayList<>(validTeam(m))), c, vc);
    }

    @Test
    void joiningTakesTheFeeFillsASlotAndSendsAnEmail() throws Exception {
        User u = contests.fundedUser("Rohit", "100.00");
        String token = obtainAccessToken(u.getUserId(), UserFixtures.PASSWORD);
        String body = objectMapper.writeValueAsString(team(m, m.home("bat1"), m.away("bowl1")));

        mockMvc.perform(post("/contests/" + contest.getId() + "/entries").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryFeePaid").value(49.5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("50.50");
        assertThat(contestRepository.findById(contest.getId()).orElseThrow().getCurrentEntries()).isEqualTo(1);
        assertThat(emails.sentTo(u.getEmail())).anyMatch(e -> e.subject().contains(contest.getName()));

        mockMvc.perform(get("/contests/" + contest.getId() + "/entries/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(11));
        mockMvc.perform(get("/contests/" + contest.getId() + "/prizes"))
                .andExpect(jsonPath("$[0].amount").value(90.0));
    }

    @Test
    void teamSelectionRules() {
        User u = contests.fundedUser("Jas", "500");
        String c = m.home("bat1");
        String vc = m.away("bowl1");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> t.subList(0, 10), c, vc)))
                .hasMessageContaining("exactly 11");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> {
            t.set(t.indexOf(m.away("bat1")), m.home("bat4"));
            return t;
        }, c, vc))).hasMessageContaining("At most 7");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> {
            t.set(t.indexOf(m.home("wk1")), m.away("bat2"));
            return t;
        }, c, vc))).hasMessageContaining("WK");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> {
            t.set(0, "nobody");
            return t;
        }, c, vc))).hasMessageContaining("not in either squad");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> t, m.away("wk1"), vc)))
                .hasMessageContaining("among the picked");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> t, c, c)))
                .hasMessageContaining("different players");
        assertThat(walletService.getBalance(u.getId())).isEqualByComparingTo("500.00");
        assertThat(contestRepository.findById(contest.getId()).orElseThrow().getCurrentEntries()).isZero();
    }

    @Test
    void creditLimitAllowsExactlyOneHundred() {
        // Add two stars to the home squad: 12.0 brings the team to exactly 100.0 credits, 12.5 to 100.5.
        List<String> squad = squadService.getSquad(m.matchId()).stream()
                .filter(s -> s.teamId().equals(m.home())).map(SquadMember::playerId).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        playerRepository.insert(new Player(m.home("star"), m.home(), "Star", PlayerRole.BAT, new BigDecimal("12.0")));
        playerRepository.insert(new Player(m.home("superstar"), m.home(), "Superstar", PlayerRole.BAT, new BigDecimal("12.5")));
        squad.add(m.home("star"));
        squad.add(m.home("superstar"));
        squadService.setSquad(m.matchId(), new SquadRequest(m.home(), squad, List.of()));

        User u = contests.fundedUser("Hardik", "500");
        assertThatThrownBy(() -> entryService.join(u.getId(), contest.getId(), withTeam(t -> {
            t.set(t.indexOf(m.home("bat3")), m.home("superstar"));
            return t;
        }, m.home("bat1"), m.away("bowl1")))).hasMessageContaining("100.5 credits");

        ContestEntry ok = entryService.join(u.getId(), contest.getId(), withTeam(t -> {
            t.set(t.indexOf(m.home("bat3")), m.home("star"));
            return t;
        }, m.home("bat1"), m.away("bowl1")));
        assertThat(ok.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void insufficientFundsLeavesNoTrace() {
        User poor = contests.fundedUser("Poor", "49.49");
        assertThatThrownBy(() -> entryService.join(poor.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1"))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Insufficient");
        assertThat(contestRepository.findById(contest.getId()).orElseThrow().getCurrentEntries()).isZero();
        assertThat(walletService.getBalance(poor.getId())).isEqualByComparingTo("49.49");
    }

    @Test
    void duplicateFullAndLateEntriesAreRejected() {
        User a = contests.fundedUser("A", "200");
        User b = contests.fundedUser("B", "200");
        User c = contests.fundedUser("C", "200");
        entryService.join(a.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1")));
        assertThatThrownBy(() -> entryService.join(a.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1"))))
                .hasMessageContaining("already joined");
        entryService.join(b.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1")));
        assertThatThrownBy(() -> entryService.join(c.getId(), contest.getId(), team(m, m.home("bat1"), m.away("bowl1"))))
                .hasMessageContaining("full");
        assertThat(walletService.getBalance(c.getId())).isEqualByComparingTo("200.00");

        Contest other = contests.paidContest(m, "0", "0", 10, 1, null);
        clock.advance(Duration.ofHours(1));
        assertThatThrownBy(() -> entryService.join(c.getId(), other.getId(), team(m, m.home("bat1"), m.away("bowl1"))))
                .hasMessageContaining("no longer accepting");
    }
}
