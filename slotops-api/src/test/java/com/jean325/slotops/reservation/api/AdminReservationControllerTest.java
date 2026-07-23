package com.jean325.slotops.reservation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jean325.slotops.support.FixedClockConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(FixedClockConfig.class)
class AdminReservationControllerTest {

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

    private void seedPaidPayment(long reservationId) {
        jdbcTemplate.execute("""
                insert into payments (id, reservation_id, payment_status, paid_amount, refunded_amount, created_at, updated_at)
                values (%d, %d, 'PAID', 20000.00, 0, current_timestamp, current_timestamp)
                """.formatted(reservationId, reservationId));
    }

    private Map<String, Object> paymentRow(long reservationId) {
        return jdbcTemplate.queryForMap(
                "select payment_status, refunded_amount from payments where reservation_id = " + reservationId);
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
    void cancellingFarAheadPaidReservationRefundsFully() throws Exception {
        // Clock is frozen at 2026-05-01; reservation starts 2026-06-15 (far > 24h ahead).
        seedReservation(1, "RSV-1", "CONFIRMED");
        seedPaidPayment(1);

        mockMvc.perform(post("/api/v1/organizations/1/reservations/1/cancel")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Map<String, Object> payment = paymentRow(1);
        assertThat(payment.get("payment_status")).isEqualTo("REFUNDED");
        assertThat(((Number) payment.get("refunded_amount")).doubleValue()).isEqualTo(20000.0);
    }

    @Test
    void cancellingWithinTwoHoursRefundsNothing() throws Exception {
        // Reservation starts 2026-05-01 09:30 (+09:00), 30 min after the frozen 09:00 now.
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    2, 1, 1, 1, 1, 'RSV-2', 'CONFIRMED',
                    TIMESTAMP WITH TIME ZONE '2026-05-01 09:30:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-05-01 10:30:00+09:00',
                    2, 20000.00, 0, 0, 20000.00,
                    current_timestamp, current_timestamp
                )
                """);
        seedPaidPayment(2);

        mockMvc.perform(post("/api/v1/organizations/1/reservations/2/cancel")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Map<String, Object> payment = paymentRow(2);
        assertThat(payment.get("payment_status")).isEqualTo("PAID");
        assertThat(((Number) payment.get("refunded_amount")).doubleValue()).isEqualTo(0.0);
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
    void filtersListByDateRange() throws Exception {
        seedReservation(1, "RSV-1", "PENDING"); // starts 2026-06-15 10:00+09:00
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    2, 1, 1, 1, 1, 'RSV-2', 'PENDING',
                    TIMESTAMP WITH TIME ZONE '2026-07-20 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-07-20 12:00:00+09:00',
                    2, 20000.00, 0, 0, 20000.00,
                    current_timestamp, current_timestamp
                )
                """);

        // 'to' is exclusive, so a June-only window keeps RSV-1 and drops RSV-2.
        mockMvc.perform(get("/api/v1/organizations/1/reservations")
                        .param("from", "2026-06-01T00:00:00+09:00")
                        .param("to", "2026-07-01T00:00:00+09:00")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reservationCode").value("RSV-1"));
    }

    @Test
    void filtersListByCustomer() throws Exception {
        jdbcTemplate.execute("""
                insert into customers (id, organization_id, name, email, phone, created_at, updated_at)
                values (2, 1, 'Other Guest', 'other@example.com', '010-9999-9999', current_timestamp, current_timestamp)
                """);
        seedReservation(1, "RSV-1", "PENDING"); // customer 1
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    2, 1, 1, 1, 2, 'RSV-2', 'PENDING',
                    TIMESTAMP WITH TIME ZONE '2026-06-16 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-06-16 12:00:00+09:00',
                    2, 20000.00, 0, 0, 20000.00,
                    current_timestamp, current_timestamp
                )
                """);

        mockMvc.perform(get("/api/v1/organizations/1/reservations")
                        .param("customerId", "2")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reservationCode").value("RSV-2"));
    }

    @Test
    void rejectsInvertedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1/reservations")
                        .param("from", "2026-07-01T00:00:00+09:00")
                        .param("to", "2026-06-01T00:00:00+09:00")
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
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
