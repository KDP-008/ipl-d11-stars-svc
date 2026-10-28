package com.ipld11stars.backend.support;

import com.ipld11stars.backend.dto.DeliveryRequest;
import com.ipld11stars.backend.dto.SquadRequest;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.Player;
import com.ipld11stars.backend.model.PlayerRole;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.repository.jdbc.PlayerRepository;
import com.ipld11stars.backend.service.MatchSquadService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds isolated matches with two full squads. Every call uses fresh ids, so tests sharing the
 * Spring context and H2 database never collide.
 */
@Component
public class MatchFixtures {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchSquadService squadService;

    public MatchFixtures(MatchRepository matchRepository, PlayerRepository playerRepository,
                         MatchSquadService squadService) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.squadService = squadService;
    }

    /** A match with squads for both sides. Squad layout per side: wk1, bat1-4, ar1-2, bowl1-4 (11 players). */
    public TestMatch createMatch(LocalDateTime startTimeUtc) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String home = "H" + suffix;
        String away = "A" + suffix;
        Match match = new Match(1, home, away, "2026", startTimeUtc, "Test Ground");
        match.setMatchStatus("UPCOMING");
        match.setMatchType("LEAGUE");
        matchRepository.save(match);
        TestMatch tm = new TestMatch(match.getId(), home, away);
        createSquad(tm, home);
        createSquad(tm, away);
        return tm;
    }

    private void createSquad(TestMatch tm, String teamId) {
        List<String> ids = new ArrayList<>();
        addPlayers(ids, teamId, "wk", 1, PlayerRole.WK, "9.0");
        addPlayers(ids, teamId, "bat", 4, PlayerRole.BAT, "9.0");
        addPlayers(ids, teamId, "ar", 2, PlayerRole.AR, "9.0");
        addPlayers(ids, teamId, "bowl", 4, PlayerRole.BOWL, "8.5");
        squadService.setSquad(tm.matchId(), new SquadRequest(teamId, ids, ids));
    }

    private void addPlayers(List<String> ids, String teamId, String prefix, int count, PlayerRole role, String credits) {
        for (int i = 1; i <= count; i++) {
            String id = teamId + "-" + prefix + i;
            playerRepository.insert(new Player(id, teamId, id, role, new BigDecimal(credits)));
            ids.add(id);
        }
    }

    public record TestMatch(String matchId, String home, String away) {
        public String p(String team, String slot) {
            return team + "-" + slot;
        }

        public String home(String slot) {
            return p(home, slot);
        }

        public String away(String slot) {
            return p(away, slot);
        }
    }

    // ---- delivery builders -----------------------------------------------------------------

    public static DeliveryRequest ball(int innings, int over, String batting, String striker, String nonStriker,
                                       String bowler, int runs) {
        return new DeliveryRequest(innings, over, null, batting, striker, nonStriker, bowler, runs,
                ExtrasType.NONE, 0, null);
    }

    public static DeliveryRequest extra(int innings, int over, String batting, String striker, String nonStriker,
                                        String bowler, ExtrasType type, int runsBatter, int extrasRuns) {
        return new DeliveryRequest(innings, over, null, batting, striker, nonStriker, bowler, runsBatter,
                type, extrasRuns, null);
    }

    public static DeliveryRequest wicket(int innings, int over, String batting, String striker, String nonStriker,
                                         String bowler, DismissalType type, String out, String fielder) {
        return new DeliveryRequest(innings, over, null, batting, striker, nonStriker, bowler, 0,
                ExtrasType.NONE, 0, new DeliveryRequest.Wicket(type, out, fielder));
    }
}
