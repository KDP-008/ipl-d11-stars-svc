package com.ipld11stars.backend.service;

import com.ipld11stars.backend.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service to fetch IPL match schedule from external APIs and cache it.
 * Cache is refreshed every day at 2:00 AM IST.
 */
@Service
public class IplApiService {

    private static final Logger logger = LoggerFactory.getLogger(IplApiService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MatchService matchService;

    @Value("${ipl.api.url:https://api.cricketdata.org/cricket/match-info?apikey=YOUR_API_KEY}")
    private String iplApiUrl;

    @Value("${ipl.enable.external.api:false}")
    private boolean enableExternalApi;

    @jakarta.annotation.PostConstruct
    public void initScheduleIfEmpty() {
        try {
            if (matchService.getAllMatches().isEmpty()) {
                logger.info("No matches found in database on startup. Initializing mock IPL schedule...");
                createMockIplSchedule();
            }
        } catch (Exception e) {
            logger.warn("Could not check or initialize mock IPL schedule on startup: {}", e.getMessage());
        }
    }

    /**
     * Fetch IPL matches from external API.
     * This method is called automatically every day at 2:00 AM IST.
     * Manual refresh can be triggered through the API.
     */
    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Kolkata")  // 2:00 AM IST (20:30 UTC previous day)
    @CacheEvict(value = "ipl-schedule", allEntries = true)
    public void syncIplScheduleDaily() {
        logger.info("Starting daily IPL schedule sync...");
        try {
            if (enableExternalApi) {
                fetchAndSyncFromExternalApi();
            } else {
                logger.info("External API disabled. Using fallback/database matches.");
            }
        } catch (Exception e) {
            logger.error("Error syncing IPL schedule: ", e);
        }
    }

    /**
     * Refresh IPL schedule on demand (admin endpoint can call this)
     */
    public void refreshScheduleNow() {
        logger.info("Manual refresh of IPL schedule triggered");
        try {
            if (enableExternalApi) {
                fetchAndSyncFromExternalApi();
            } else {
                logger.warn("External API disabled. Cannot refresh from external source.");
            }
        } catch (Exception e) {
            logger.error("Error refreshing IPL schedule: ", e);
        }
    }

    /**
     * Fetch matches from CricketData API or similar service
     */
    private void fetchAndSyncFromExternalApi() {
        logger.info("Fetching IPL matches from external API: {}", iplApiUrl);

        try {
            if (iplApiUrl == null || iplApiUrl.contains("YOUR_API_KEY")) {
                logger.warn("IPL API URL not configured. Using mock data.");
                createMockIplSchedule();
                return;
            }

            String rawResponse = restTemplate.getForObject(iplApiUrl, String.class);
            if (rawResponse == null || rawResponse.isBlank()) {
                logger.warn("Received empty response from IPL API. Using fallback.");
                createMockIplSchedule();
                return;
            }

            String jsonPayload = extractJsonPayload(rawResponse);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> response = objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {
            });

            List<Map<String, Object>> matches = extractMatchList(response);
            if (!matches.isEmpty()) {
                processFetchedMatches(matches);
                logger.info("Successfully synced {} matches from external API", matches.size());
            } else {
                logger.warn("No matches found in API response. Using fallback.");
                createMockIplSchedule();
            }

        } catch (Exception e) {
            logger.error("Failed to fetch from external API: {}. Using fallback data.", e.getMessage());
            createMockIplSchedule();
        }
    }

    private String extractJsonPayload(String rawResponse) {
        String trimmed = rawResponse.trim();
        String prefix = "MatchSchedule(";

        if (trimmed.startsWith(prefix) && trimmed.endsWith(");")) {
            return trimmed.substring(prefix.length(), trimmed.length() - 2).trim();
        }

        if (trimmed.startsWith(prefix) && trimmed.endsWith(")")) {
            return trimmed.substring(prefix.length(), trimmed.length() - 1).trim();
        }

        int openParen = trimmed.indexOf('(');
        int closeParen = trimmed.lastIndexOf(')');
        if (openParen > 0 && closeParen > openParen) {
            return trimmed.substring(openParen + 1, closeParen).trim();
        }

        return trimmed;
    }

    private List<Map<String, Object>> extractMatchList(Map<String, Object> response) {
        if (response == null) {
            return Collections.emptyList();
        }

        Object matchSummary = response.get("Matchsummary");
        if (matchSummary instanceof List) {
            return (List<Map<String, Object>>) matchSummary;
        }

        Object matches = response.get("matches");
        if (matches instanceof List) {
            return (List<Map<String, Object>>) matches;
        }

        return Collections.emptyList();
    }

    /**
     * Process matches fetched from external API
     */
    private void processFetchedMatches(List<Map<String, Object>> apiMatches) {
        List<Match> matchesToSave = new ArrayList<>();

        for (Map<String, Object> apiMatch : apiMatches) {
            try {
                Match match = parseApiMatch(apiMatch);
                matchesToSave.add(match);
            } catch (Exception e) {
                logger.warn("Failed to parse match data: {}", e.getMessage());
            }
        }

        // Save all matches to database
        for (Match match : matchesToSave) {
            try {
                matchService.createOrUpdateMatch(match);
            } catch (Exception e) {
                logger.warn("Failed to save match: {}", e.getMessage());
            }
        }
    }

    /**
     * Parse match data from external API response
     */
    private Match parseApiMatch(Map<String, Object> apiMatch) {
        Match match = new Match();

        match.setMatchNumber(parseInteger(apiMatch, "RowNo", "MatchID", "MatchNumber"));
        match.setSeasonId(parseString(apiMatch, "CompetitionName", "season", "MatchDate"));
        match.setHomeTeamId(parseString(apiMatch, "FirstBattingTeamCode", "FirstBattingTeamName", "FirstBattingTeamID"));
        match.setAwayTeamId(parseString(apiMatch, "SecondBattingTeamCode", "SecondBattingTeamName", "SecondBattingTeamID"));
        match.setVenue(parseString(apiMatch, "GroundName", "venue"));

        String startTime = parseString(apiMatch, "MATCH_COMMENCE_START_DATE");
        if (startTime.isEmpty()) {
            String date = parseString(apiMatch, "MatchDate");
            String time = parseString(apiMatch, "MatchTime");
            if (!date.isEmpty() && !time.isEmpty()) {
                startTime = date + " " + time;
            }
        }

        if (!startTime.isEmpty()) {
            try {
                if (startTime.contains("T")) {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(startTime);
                    match.setStartTimeUtc(zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime localDateTime = LocalDateTime.parse(startTime, formatter);
                    match.setStartTimeUtc(localDateTime.atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
                }
            } catch (Exception e) {
                logger.warn("Failed to parse match date/time: {}", startTime);
            }
        }

        match.setMatchType(parseString(apiMatch, "MatchType", "MatchTypeName", "match_type"));
        match.setMatchStatus(parseString(apiMatch, "MatchStatus", "status").toUpperCase());

        return match;
    }

    private String parseString(Map<String, Object> apiMatch, String... keys) {
        for (String key : keys) {
            Object value = apiMatch.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str.trim();
            }
            if (value instanceof Number) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private Integer parseInteger(Map<String, Object> apiMatch, String... keys) {
        for (String key : keys) {
            Object value = apiMatch.get(key);
            if (value instanceof Number num) {
                return num.intValue();
            }
            if (value instanceof String str) {
                try {
                    return Integer.parseInt(str.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    /**
     * Create mock IPL schedule for development/testing
     * This provides realistic IPL match data for local development
     */
    public void createMockIplSchedule() {
        logger.info("Creating mock IPL schedule for development");

        List<Match> mockMatches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Mock match 1
        Match match1 = new Match(1, "CSK", "MI", "2024", now.plusDays(1), "Chepauk, Chennai");
        mockMatches.add(match1);

        // Mock match 2
        Match match2 = new Match(2, "RCB", "KKR", "2024", now.plusDays(2), "M. Chinnaswamy Stadium, Bangalore");
        mockMatches.add(match2);

        // Mock match 3
        Match match3 = new Match(3, "DC", "SRH", "2024", now.plusDays(3), "Arun Jaitley Stadium, Delhi");
        mockMatches.add(match3);

        // Mock match 4
        Match match4 = new Match(4, "LSG", "GT", "2024", now.plusDays(4), "Bharat Ratna Sardar Vallabhbhai Patel International Stadium, Ahmedabad");
        mockMatches.add(match4);

        // Mock match 5
        Match match5 = new Match(5, "RR", "PBKS", "2024", now.plusDays(5), "Sawai Mansingh Stadium, Jaipur");
        mockMatches.add(match5);

        // Mock match 6
        Match match6 = new Match(6, "MI", "CSK", "2024", now.plusDays(6), "Wankhede Stadium, Mumbai");
        mockMatches.add(match6);

        // Save all mock matches
        for (Match match : mockMatches) {
            try {
                matchService.createOrUpdateMatch(match);
            } catch (Exception e) {
                logger.warn("Failed to save mock match: {}", e.getMessage());
            }
        }

        logger.info("Mock IPL schedule created with {} matches", mockMatches.size());
    }

    /**
     * Get available cricket data providers
     * Currently supports: CricketData.org API (with API key)
     */
    public String getApiProviderInfo() {
        return """
            IPL Schedule Sync Service
            ==========================
            Current Status: %s
            
            Supported Providers:
            1. CricketData.org API (Free tier with 1000 requests/day)
               - Requires API key in application.properties
               - Set: ipl.api.url=https://api.cricketdata.org/cricket/match-info?apikey=YOUR_API_KEY
               - Set: ipl.enable.external.api=true
            
            2. Mock/Development Mode (Default)
               - Provides realistic mock IPL matches for development
               - Uses local database as cache
            
            Cache Refresh Schedule: Daily at 2:00 AM IST
            Last Sync: Check database for latest match update timestamps
            """.formatted(enableExternalApi ? "EXTERNAL API ENABLED" : "MOCK MODE");
    }
}
