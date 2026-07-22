package com.jean325.slotops.availability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Sql("/sql/clean.sql")
class AvailabilityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                insert into users (id, email, password_hash, name, status, created_at, updated_at)
                values (1, 'member@example.com', 'x', 'Member', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into users (id, email, password_hash, name, status, created_at, updated_at)
                values (2, 'outsider@example.com', 'x', 'Outsider', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1, 'Org', 'org', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at)
                values (1, 1, 'OWNER', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into locations (id, organization_id, name, timezone, status, created_at, updated_at)
                values (1, 1, 'Studio', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    1, 1, 1, 'ROOM', 'Room', 4, 60, 60, 180, 0,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """);
    }

    @Test
    void createsAvailabilityRule() throws Exception {
        String body = """
                {
                  "dayOfWeek": 1,
                  "startTime": "09:00",
                  "endTime": "18:00",
                  "isOpen": true,
                  "leadTimeMinutes": 60
                }
                """;
        mockMvc.perform(post("/api/v1/organizations/1/resources/1/availability-rules")
                        .requestAttr("userId", 1L)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayOfWeek").value(1))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.isOpen").value(true));
    }

    @Test
    void listsAvailabilityRules() throws Exception {
        jdbcTemplate.execute("""
                insert into availability_rules (
                    resource_id, day_of_week, start_time, end_time, is_open,
                    lead_time_minutes, created_at, updated_at
                ) values (1, 2, '10:00', '17:00', true, 0, current_timestamp, current_timestamp)
                """);
        mockMvc.perform(get("/api/v1/organizations/1/resources/1/availability-rules")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void createsBlackoutPeriod() throws Exception {
        String body = """
                {
                  "startAt": "2026-06-01T00:00:00+09:00",
                  "endAt": "2026-06-01T23:59:59+09:00",
                  "reason": "Holiday"
                }
                """;
        mockMvc.perform(post("/api/v1/organizations/1/resources/1/blackout-periods")
                        .requestAttr("userId", 1L)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reason").value("Holiday"));
    }

    @Test
    void rejectsNonMemberWithForbidden() throws Exception {
        String body = """
                {
                  "dayOfWeek": 1,
                  "startTime": "09:00",
                  "endTime": "18:00",
                  "isOpen": true,
                  "leadTimeMinutes": 60
                }
                """;
        mockMvc.perform(post("/api/v1/organizations/1/resources/1/availability-rules")
                        .requestAttr("userId", 2L)
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }
}
