package com.jean325.slotops.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Freezes {@link Clock} at a fixed instant before every fixture date so that
 * time-based availability rules (past-date, lead-time, same-day cutoff) are
 * deterministic. Import into any @SpringBootTest that exercises those rules.
 */
@TestConfiguration
public class FixedClockConfig {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-05-01T00:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneId.of("Asia/Seoul"));
    }
}
