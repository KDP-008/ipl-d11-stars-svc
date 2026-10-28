package com.ipld11stars.backend.data;

import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.UserRole;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.repository.jdbc.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds initial test and master data for offline testing and local development.
 * Idempotent: checks for existence before inserting records.
 */
@Component
public class TestDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestDataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            seedTeams();
            seedUsers();
            seedMatches();
            seedContests();
            seedPlayers();
            logger.info("Test data seeding completed successfully.");
        } catch (Exception e) {
            logger.warn("Test data seeding encountered an issue (safe to ignore if already seeded): {}", e.getMessage());
        }
    }

    private void seedTeams() {
        String countSql = "SELECT COUNT(*) FROM ipld11stars.teams";
        try {
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            if (count != null && count > 0) {
                return;
            }

            String insertSql = "INSERT INTO ipld11stars.teams (id, name, short_name, slug, logo_url, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

            Object[][] teams = {
                    {"CSK", "Chennai Super Kings", "CSK", "chennai-super-kings", "https://scores.iplt20.com/ipl/teamlogos/CSK.png"},
                    {"MI", "Mumbai Indians", "MI", "mumbai-indians", "https://scores.iplt20.com/ipl/teamlogos/MI.png"},
                    {"RCB", "Royal Challengers Bengaluru", "RCB", "royal-challengers-bengaluru", "https://scores.iplt20.com/ipl/teamlogos/rcb.png"},
                    {"KKR", "Kolkata Knight Riders", "KKR", "kolkata-knight-riders", "https://scores.iplt20.com/ipl/teamlogos/KKR.png"},
                    {"DC", "Delhi Capitals", "DC", "delhi-capitals", "https://scores.iplt20.com/ipl/teamlogos/DC.png"},
                    {"GT", "Gujarat Titans", "GT", "gujarat-titans", "https://scores.iplt20.com/ipl/teamlogos/GT.png"},
                    {"RR", "Rajasthan Royals", "RR", "rajasthan-royals", "https://scores.iplt20.com/ipl/teamlogos/RR.png"},
                    {"PBKS", "Punjab Kings", "PBKS", "punjab-kings", "https://scores.iplt20.com/ipl/teamlogos/PBKS.png"},
                    {"SRH", "Sunrisers Hyderabad", "SRH", "sunrisers-hyderabad", "https://scores.iplt20.com/ipl/teamlogos/SRH.png"},
                    {"LSG", "Lucknow Super Giants", "LSG", "lucknow-super-giants", "https://scores.iplt20.com/ipl/teamlogos/LSG.png"}
            };

            for (Object[] team : teams) {
                jdbcTemplate.update(insertSql, team[0], team[1], team[2], team[3], team[4]);
            }
            logger.info("Seeded {} IPL teams.", teams.length);
        } catch (Exception e) {
            logger.debug("Teams seeding skipped: {}", e.getMessage());
        }
    }

    private void seedUsers() {
        if (!userRepository.existsByUserId("testplayer")) {
            User testUser = new User();
            testUser.setUserId("testplayer");
            testUser.setFirstName("Test");
            testUser.setLastName("Player");
            testUser.setEmail("testplayer@ipld11stars.com");
            testUser.setPasswordHash(passwordEncoder.encode("password123"));
            testUser.setRole(UserRole.PLAYER);
            testUser.setStatus("ACTIVE");
            userRepository.save(testUser);
            logger.info("Seeded user 'testplayer' with password 'password123'");
        }

        if (!userRepository.existsByUserId("contestadmin")) {
            User adminUser = new User();
            adminUser.setUserId("contestadmin");
            adminUser.setFirstName("Contest");
            adminUser.setLastName("Admin");
            adminUser.setEmail("admin@ipld11stars.com");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setStatus("ACTIVE");
            userRepository.save(adminUser);
            logger.info("Seeded user 'contestadmin' with password 'admin123'");
        }
    }

    private void seedMatches() {
        if (matchRepository.findAll().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();

            Match match1 = new Match(1, "CSK", "MI", "2026", now.plusDays(1), "Chepauk, Chennai");
            matchRepository.save(match1);

            Match match2 = new Match(2, "RCB", "KKR", "2026", now.plusDays(2), "M. Chinnaswamy Stadium, Bangalore");
            matchRepository.save(match2);

            Match match3 = new Match(3, "DC", "SRH", "2026", now.plusDays(3), "Arun Jaitley Stadium, Delhi");
            matchRepository.save(match3);

            logger.info("Seeded initial matches for testing.");
        }
    }

    private void seedContests() {
        if (contestRepository.findAll().isEmpty()) {
            userRepository.findByUserId("contestadmin").ifPresent(admin -> {
                matchRepository.findAll().stream().findFirst().ifPresent(match -> {
                    Contest contest1 = new Contest(
                            "Grand League 2026",
                            match.getId(),
                            admin.getId(),
                            "FREE",
                            100,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(1000),
                            3
                    );
                    contestRepository.save(contest1);

                    Contest contest2 = new Contest(
                            "Head to Head Challenge",
                            match.getId(),
                            admin.getId(),
                            "FREE",
                            2,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(50),
                            1
                    );
                    contestRepository.save(contest2);

                    logger.info("Seeded initial contests for testing.");
                });
            });
        }
    }

    private void seedPlayers() {
        String countSql = "SELECT COUNT(*) FROM ipld11stars.players";
        try {
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            if (count != null && count > 0) {
                return;
            }

            String insertSql = "INSERT INTO ipld11stars.players (id, team_id, full_name, role, credits, nationality, batting_style, bowling_style, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            Object[][] players = {
                    {"ply-01", "RCB", "Virat Kohli", "BAT", 10.5, "Indian", "Right-hand bat", "Right-arm medium"},
                    {"ply-02", "CSK", "MS Dhoni", "WK", 9.0, "Indian", "Right-hand bat", "Right-arm medium"},
                    {"ply-03", "MI", "Rohit Sharma", "BAT", 9.5, "Indian", "Right-hand bat", "Right-arm offbreak"},
                    {"ply-04", "MI", "Jasprit Bumrah", "BOWL", 10.0, "Indian", "Right-hand bat", "Right-arm fast"},
                    {"ply-05", "CSK", "Ravindra Jadeja", "AR", 9.5, "Indian", "Left-hand bat", "Slow left-arm orthodox"},
                    {"ply-06", "MI", "Hardik Pandya", "AR", 9.0, "Indian", "Right-hand bat", "Right-arm fast-medium"},
                    {"ply-07", "DC", "Rishabh Pant", "WK", 9.5, "Indian", "Left-hand bat", "Wicketkeeper"},
                    {"ply-08", "GT", "Rashid Khan", "BOWL", 9.5, "Afghan", "Right-hand bat", "Right-arm legbreak"},
                    {"ply-09", "GT", "Shubman Gill", "BAT", 9.5, "Indian", "Right-hand bat", "Right-arm offbreak"},
                    {"ply-10", "RR", "Sanju Samson", "WK", 9.0, "Indian", "Right-hand bat", "Wicketkeeper"}
            };

            for (Object[] player : players) {
                jdbcTemplate.update(insertSql, player[0], player[1], player[2], player[3], player[4], player[5], player[6], player[7]);
            }
            logger.info("Seeded {} IPL players.", players.length);
        } catch (Exception e) {
            logger.debug("Players seeding skipped: {}", e.getMessage());
        }
    }
}
