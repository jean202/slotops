package com.jean325.slotops.dashboard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class DashboardControllerTest {

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
                    1, 1, 1, 'ROOM', 'Room A', 4, 60, 60, 180, 0,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                insert into customers (id, organization_id, name, email, phone, created_at, updated_at)
                values (1, 1, 'Kim Minji', 'minji@example.com', '010-1234-5678', current_timestamp, current_timestamp)
                """);
        // Two CONFIRMED (revenue 20000 each), one COMPLETED (30000), one CANCELLED (excluded).
        seedReservation(1, "RSV-1", "CONFIRMED", "2026-06-10", "20000.00");
        seedReservation(2, "RSV-2", "CONFIRMED", "2026-06-11", "20000.00");
        seedReservation(3, "RSV-3", "COMPLETED", "2026-06-12", "30000.00");
        seedReservation(4, "RSV-4", "CANCELLED", "2026-06-13", "20000.00");
        // Outside a June window.
        seedReservation(5, "RSV-5", "CONFIRMED", "2026-07-20", "99000.00");
    }

    private void seedReservation(long id, String code, String status, String date, String total) {
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    %d, 1, 1, 1, 1, '%s', '%s',
                    TIMESTAMP WITH TIME ZONE '%s 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '%s 12:00:00+09:00',
                    2, %s, 0, 0, %s,
                    current_timestamp, current_timestamp
                )
                """.formatted(id, code, status, date, date, total, total));
    }

    @Test
    void summarizesCountsAndRevenueWithinRange() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1/dashboard/summary")
                        .param("from", "2026-06-01T00:00:00+09:00")
                        .param("to", "2026-07-01T00:00:00+09:00")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReservations").value(4))
                .andExpect(jsonPath("$.data.reservationsByStatus.CONFIRMED").value(2))
                .andExpect(jsonPath("$.data.reservationsByStatus.COMPLETED").value(1))
                .andExpect(jsonPath("$.data.reservationsByStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.data.reservationsByStatus.PENDING").value(0))
                // 20000 + 20000 + 30000, cancelled excluded, July excluded
                .andExpect(jsonPath("$.data.confirmedRevenue").value(70000.0));
    }

    @Test
    void rejectsNonMember() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1/dashboard/summary")
                        .requestAttr("userId", 2L))
                .andExpect(status().isForbidden());
    }
}
