package com.ipld11stars.backend;

import com.ipld11stars.backend.dto.MatchDTO;
import com.ipld11stars.backend.service.IplApiService;
import com.ipld11stars.backend.service.MatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IplApiServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IplApiService iplApiService;

    @Autowired
    private MatchService matchService;

    @Test
    @DisplayName("Offline mock schedule creation should populate database with matches")
    void testMockScheduleCreationOffline() {
        iplApiService.createMockIplSchedule();

        List<MatchDTO> matches = matchService.getAllMatches();
        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        assertTrue(matches.size() >= 6, "Expected at least 6 mock matches to be created");
    }

    @Test
    @DisplayName("API provider info should report current offline mode status")
    void testApiProviderInfo() {
        String info = iplApiService.getApiProviderInfo();
        assertNotNull(info);
        assertTrue(info.contains("IPL Schedule Sync Service"));
        assertTrue(info.contains("MOCK MODE"));
    }
}
