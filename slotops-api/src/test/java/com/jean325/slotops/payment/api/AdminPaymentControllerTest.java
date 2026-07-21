package com.jean325.slotops.payment.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AdminPaymentControllerTest {

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
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    1, 1, 1, 1, 1, 'RSV-1', 'CONFIRMED',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                    2, 20000.00, 0, 0, 20000.00,
                    current_timestamp, current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                insert into payments (id, reservation_id, payment_status, paid_amount, refunded_amount, created_at, updated_at)
                values (1, 1, 'UNPAID', 0, 0, current_timestamp, current_timestamp)
                """);
    }

    @Test
    void returnsPaymentForReservation() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1/reservations/1/payment")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNPAID"))
                .andExpect(jsonPath("$.data.reservationId").value(1));
    }

    @Test
    void marksPaymentPaid() throws Exception {
        String body = """
                { "status": "PAID", "transactionRef": "TX-123" }
                """;
        mockMvc.perform(put("/api/v1/organizations/1/reservations/1/payment")
                        .requestAttr("userId", 1L)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAmount").value(20000.0))
                .andExpect(jsonPath("$.data.transactionRef").value("TX-123"));
    }

    @Test
    void refundMovesPaidAmountToRefunded() throws Exception {
        jdbcTemplate.execute("""
                update payments set payment_status = 'PAID', paid_amount = 20000.00 where id = 1
                """);
        String body = """
                { "status": "REFUNDED" }
                """;
        mockMvc.perform(put("/api/v1/organizations/1/reservations/1/payment")
                        .requestAttr("userId", 1L)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAmount").value(20000.0));
    }

    @Test
    void rejectsNonMember() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1/reservations/1/payment")
                        .requestAttr("userId", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidStatus() throws Exception {
        String body = """
                { "status": "BOGUS" }
                """;
        mockMvc.perform(put("/api/v1/organizations/1/reservations/1/payment")
                        .requestAttr("userId", 1L)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }
}
