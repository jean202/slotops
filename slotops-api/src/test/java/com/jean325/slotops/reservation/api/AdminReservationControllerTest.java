package com.jean325.slotops.reservation.api;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class AdminReservationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from payments");
        jdbcTemplate.execute("delete from reservations");
        jdbcTemplate.execute("delete from customers");
        jdbcTemplate.execute("delete from availability_rules");
        jdbcTemplate.execute("delete from blackout_periods");
        jdbcTemplate.execute("delete from resources");
        jdbcTemplate.execute("delete from locations");
        jdbcTemplate.execute("delete from organization_members");
        jdbcTemplate.execute("delete from organizations");
        jdbcTemplate.execute("delete from users");

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
    }

    private void seedReservation(long id, String code, String status) {
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    %d, 1, 1, 1, 1, '%s', '%s',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                    2, 20000.00, 0, 0, 20000.00,
                    current_timestamp, current_timestamp
                )
                """.formatted(id, code, status));
    }

    @Test
    void confirmsPendingReservation() throws Exception {
        seedReservation(1, "RSV-1", "PENDING");
        mockMvc.perform(post("/api/v1/organizations/1/reservations/1/confirm")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.reservationCode").value("RSV-1"));
    }

    @Test
    void cancelsConfirmedReservation() throws Exception {
        seedReservation(1, "RSV-1", "CONFIRMED");
        mockMvc.perform(post("/api/v1/organizations/1/reservations/1/cancel")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void rejectsInvalidTransition() throws Exception {
        seedReservation(1, "RSV-1", "CONFIRMED");
        mockMvc.perform(post("/api/v1/organizations/1/reservations/1/confirm")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE_TRANSITION"));
    }

    @Test
    void rejectsNonMember() throws Exception {
        seedReservation(1, "RSV-1", "PENDING");
        mockMvc.perform(post("/api/v1/organizations/1/reservations/1/confirm")
                        .requestAttr("userId", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsNotFoundForUnknownReservation() throws Exception {
        mockMvc.perform(post("/api/v1/organizations/1/reservations/999/confirm")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void listsReservationsWithOptionalStatusFilter() throws Exception {
        seedReservation(1, "RSV-1", "PENDING");
        seedReservation(2, "RSV-2", "CONFIRMED");

        mockMvc.perform(get("/api/v1/organizations/1/reservations")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/organizations/1/reservations")
                        .param("status", "PENDING")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}
