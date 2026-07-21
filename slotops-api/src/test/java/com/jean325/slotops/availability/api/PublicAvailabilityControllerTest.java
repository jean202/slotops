package com.jean325.slotops.availability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicAvailabilityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    // Use future dates so past-date rules don't mask the behavior under test.
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate MONDAY =
            LocalDate.now(ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    private static final LocalDate SUNDAY = MONDAY.minusDays(1);

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from availability_rules");
        jdbcTemplate.execute("delete from blackout_periods");
        jdbcTemplate.execute("delete from reservations");
        jdbcTemplate.execute("delete from customers");
        jdbcTemplate.execute("delete from resources");
        jdbcTemplate.execute("delete from locations");
        jdbcTemplate.execute("delete from organization_members");
        jdbcTemplate.execute("delete from organizations");

        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1, 'Demo Org', 'demo-org', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into locations (id, organization_id, name, timezone, status, created_at, updated_at)
                values (1, 1, 'Studio A', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    1, 1, 1, 'ROOM', 'Room A', 4, 60, 60, 180, 0,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """);
        // Rule applies to Monday (ISO dayOfWeek = 1); tests target a future Monday.
        jdbcTemplate.execute("""
                insert into availability_rules (
                    resource_id, day_of_week, start_time, end_time, is_open,
                    lead_time_minutes, created_at, updated_at
                ) values (1, 1, '09:00', '18:00', true, 0, current_timestamp, current_timestamp)
                """);
    }

    @Test
    void returnsAvailableSlots() throws Exception {
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", MONDAY.toString())
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(9));
    }

    @Test
    void returnsEmptyForClosedDay() throws Exception {
        // Sunday (ISO 7) — no rule for that day → empty
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", SUNDAY.toString())
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    void returns400ForInvalidDuration() throws Exception {
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", MONDAY.toString())
                        .param("duration", "45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DURATION"));
    }

    @Test
    void blocksBlackedOutDay() throws Exception {
        jdbcTemplate.execute("""
                insert into blackout_periods (resource_id, start_at, end_at, reason, created_at)
                values (1,
                    TIMESTAMP WITH TIME ZONE '%s 00:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '%s 23:59:00+09:00',
                    'Closed', current_timestamp)
                """.formatted(MONDAY, MONDAY));
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", MONDAY.toString())
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }
}
