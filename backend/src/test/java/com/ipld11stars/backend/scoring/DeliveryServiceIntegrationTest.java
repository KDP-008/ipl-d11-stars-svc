package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.BaseIntegrationTest;
import com.ipld11stars.backend.dto.DeliveryRequest;
import com.ipld11stars.backend.dto.DeliveryResponse;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.service.DeliveryService;
import com.ipld11stars.backend.service.PlayerStatsService;
import com.ipld11stars.backend.support.MatchFixtures;
import com.ipld11stars.backend.support.MatchFixtures.TestMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ipld11stars.backend.support.MatchFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MatchFixtures fixtures;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private PlayerStatsService statsService;
    @Autowired
    private MatchRepository matchRepository;

    private TestMatch m;
    private String h;

    @BeforeEach
    void setUp() {
        m = fixtures.createMatch(LocalDateTime.now().plusHours(1));
        h = m.home();
    }

    private DeliveryResponse rec(DeliveryRequest r) {
        return deliveryService.record(m.matchId(), r);
    }

    /** Six dot balls in innings 1 by the given bowler. */
    private void dotOver(int over, String bowler) {
        for (int i = 0; i < 6; i++) {
            rec(ball(1, over, h, m.home("bat1"), m.home("bat2"), bowler, 0));
        }
    }

    @Test
    void firstDeliveryStartsTheMatchAndStatsArePersisted() {
        DeliveryResponse r = rec(ball(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), 4));
        assertThat(r.score()).isEqualTo("4/0 (0.1 ov)");
        assertThat(matchRepository.findById(m.matchId()).orElseThrow().getMatchStatus()).isEqualTo("LIVE");

        Map<String, PlayerMatchStats> stats = statsService.getStats(m.matchId()).stream()
                .collect(Collectors.toMap(PlayerMatchStats::getPlayerId, Function.identity()));
        assertThat(stats.get(m.home("bat1")).getRunsScored()).isEqualTo(4);
        // 4 runs + boundary bonus 4 + starting XI 4
        assertThat(stats.get(m.home("bat1")).getPoints()).isEqualByComparingTo("12");
        // squad members with no involvement still get the starting XI bonus
        assertThat(stats.get(m.away("bat4")).getPoints()).isEqualByComparingTo("4");
    }

    @Test
    void bowlerCannotBowlConsecutiveOversOrChangeMidOver() {
        dotOver(1, m.away("bowl1"));
        assertThatThrownBy(() -> rec(ball(1, 2, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), 0)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("consecutive");
        rec(ball(1, 2, h, m.home("bat1"), m.home("bat2"), m.away("bowl2"), 0));
        assertThatThrownBy(() -> rec(ball(1, 2, h, m.home("bat1"), m.home("bat2"), m.away("bowl3"), 0)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("being bowled by");
    }

    @Test
    void bowlerIsLimitedToFourOvers() {
        String b1 = m.away("bowl1");
        String b2 = m.away("bowl2");
        for (int over = 1; over <= 8; over++) {
            dotOver(over, over % 2 == 1 ? b1 : b2);
        }
        rec(ball(1, 9, h, m.home("bat1"), m.home("bat2"), m.away("bowl3"), 0));
        for (int i = 0; i < 5; i++) {
            rec(ball(1, 9, h, m.home("bat1"), m.home("bat2"), m.away("bowl3"), 0));
        }
        assertThatThrownBy(() -> rec(ball(1, 10, h, m.home("bat1"), m.home("bat2"), b1, 0)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("at most 4 overs");
    }

    @Test
    void freeHitFollowsANoBallAndOnlyAllowsRunOuts() {
        DeliveryResponse nb = rec(extra(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), ExtrasType.NO_BALL, 0, 1));
        assertThat(nb.freeHitNext()).isTrue();
        // a wide on the free hit keeps it pending
        DeliveryResponse wide = rec(extra(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), ExtrasType.WIDE, 0, 1));
        assertThat(wide.freeHitNext()).isTrue();
        assertThatThrownBy(() -> rec(wicket(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"),
                DismissalType.BOWLED, m.home("bat1"), null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("free hit");
        DeliveryResponse runOut = rec(wicket(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"),
                DismissalType.RUN_OUT, m.home("bat2"), m.away("ar1")));
        assertThat(runOut.delivery().freeHit()).isTrue();
        assertThat(runOut.freeHitNext()).isFalse();
        assertThat(runOut.score()).isEqualTo("2/1 (0.1 ov)");
    }

    @Test
    void dismissalRulesForNonStrikerAndExtras() {
        assertThatThrownBy(() -> rec(wicket(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"),
                DismissalType.CAUGHT, m.home("bat2"), m.away("ar1"))))
                .hasMessageContaining("non-striker");
        assertThatThrownBy(() -> rec(new DeliveryRequest(1, 1, null, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"),
                0, ExtrasType.WIDE, 1, new DeliveryRequest.Wicket(DismissalType.LBW, m.home("bat1"), null))))
                .hasMessageContaining("off a wide");
        assertThatThrownBy(() -> rec(wicket(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"),
                DismissalType.CAUGHT, m.home("bat1"), null)))
                .hasMessageContaining("fielder");

        rec(wicket(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), DismissalType.BOWLED, m.home("bat1"), null));
        assertThatThrownBy(() -> rec(ball(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), 1)))
                .hasMessageContaining("dismissed batter");
    }

    @Test
    void secondInningsWaitsForTheFirstAndEndsWhenTheTargetIsPassed() {
        assertThatThrownBy(() -> rec(ball(2, 1, m.away(), m.away("bat1"), m.away("bat2"), m.home("bowl1"), 0)))
                .hasMessageContaining("first innings is complete");

        // Bowl the home side out in the first over: ten wickets, one run.
        String[] order = {"wk1", "bat1", "bat2", "bat3", "bat4", "ar1", "ar2", "bowl1", "bowl2", "bowl3", "bowl4"};
        rec(ball(1, 1, h, m.home("wk1"), m.home("bat1"), m.away("bowl1"), 1));
        String nonStriker = m.home("wk1");
        for (int i = 1; i <= 10; i++) {
            String striker = m.home(order[i]);
            // wickets fall across overs 1 and 2 so each over stays at six legal balls
            int over = i <= 5 ? 1 : 2;
            String bowler = over == 1 ? m.away("bowl1") : m.away("bowl2");
            rec(wicket(1, over, h, striker, nonStriker, bowler, DismissalType.BOWLED, striker, null));
        }
        assertThat(deliveryService.getInnings(m.matchId()).get(0).getWickets()).isEqualTo(10);
        assertThatThrownBy(() -> rec(ball(1, 2, h, m.home("wk1"), m.home("bat1"), m.away("bowl2"), 0)))
                .hasMessageContaining("first innings is complete");

        // Chase of 2: a boundary finishes it.
        assertThatThrownBy(() -> rec(ball(2, 1, h, m.home("wk1"), m.home("bat1"), m.away("bowl1"), 0)))
                .hasMessageContaining("already batted");
        DeliveryResponse win = rec(ball(2, 1, m.away(), m.away("bat1"), m.away("bat2"), m.home("bowl1"), 4));
        assertThat(win.inningsComplete()).isTrue();
        assertThat(deliveryService.isMatchResultReached(m.matchId())).isTrue();
        assertThatThrownBy(() -> rec(ball(2, 1, m.away(), m.away("bat1"), m.away("bat2"), m.home("bowl1"), 1)))
                .hasMessageContaining("second innings is complete");
    }

    @Test
    void scoringEndpointsRequireAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(ball(1, 1, h, m.home("bat1"), m.home("bat2"), m.away("bowl1"), 1));
        String playerToken = obtainAccessToken("testplayer", "password123");
        mockMvc.perform(post("/matches/" + m.matchId() + "/deliveries")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        String adminToken = obtainAccessToken("contestadmin", "admin123");
        mockMvc.perform(post("/matches/" + m.matchId() + "/deliveries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
