package com.jean325.slotops.reservation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jean325.slotops.support.FixedClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@Sql("/sql/clean.sql")
class PublicReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1, 'Demo Org', 'demo-org', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into locations (id, organization_id, name, timezone, status, created_at, updated_at)
                values (1, 1, 'Gangnam Studio', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, description, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    12, 1, 1, 'ROOM', 'Focus Room A', 'Quiet room', 4,
                    60, 60, 180, 15,
                    18000.00, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """);
        // 2026-06-15 is a Monday (ISO day 1); clock is frozen at 2026-05-01.
        jdbcTemplate.execute("""
                insert into availability_rules (
                    resource_id, day_of_week, start_time, end_time, is_open,
                    lead_time_minutes, created_at, updated_at
                ) values (12, 1, '09:00', '18:00', true, 0, current_timestamp, current_timestamp)
                """);
    }

    @Test
    void createsReservation() throws Exception {
        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-06-15",
                  "startTime": "10:00",
                  "durationMinutes": 120,
                  "guestCount": 3,
                  "customer": {
                    "name": "Kim Minji",
                    "email": "minji@example.com",
                    "phone": "010-1234-5678"
                  },
                  "specialRequest": "Need HDMI cable"
                }
                """;

        mockMvc.perform(post("/api/v1/public/reservations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resource.id").value(12))
                .andExpect(jsonPath("$.customer.name").value("Kim Minji"))
                .andExpect(jsonPath("$.pricing.totalAmount").value(36000.0))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
    }

    @Test
    void createsReservationWithAddonsInPricing() throws Exception {
        jdbcTemplate.execute("""
                insert into addons (id, organization_id, name, price, status, created_at, updated_at)
                values (1, 1, 'Projector', 5000.00, 'ACTIVE', current_timestamp, current_timestamp)
                """);
        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-06-15",
                  "startTime": "10:00",
                  "durationMinutes": 120,
                  "guestCount": 3,
                  "customer": {
                    "name": "Kim Minji",
                    "email": "minji@example.com",
                    "phone": "010-1234-5678"
                  },
                  "addons": [ { "addonId": 1, "quantity": 2 } ]
                }
                """;

        mockMvc.perform(post("/api/v1/public/reservations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pricing.baseAmount").value(36000.0))
                .andExpect(jsonPath("$.pricing.addonAmount").value(10000.0))
                .andExpect(jsonPath("$.pricing.totalAmount").value(46000.0));
    }

    @Test
    void rejectsReservationWithUnavailableAddon() throws Exception {
        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-06-15",
                  "startTime": "10:00",
                  "durationMinutes": 120,
                  "guestCount": 3,
                  "customer": {
                    "name": "Kim Minji",
                    "email": "minji@example.com",
                    "phone": "010-1234-5678"
                  },
                  "addons": [ { "addonId": 999, "quantity": 1 } ]
                }
                """;

        mockMvc.perform(post("/api/v1/public/reservations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADDON_UNAVAILABLE"));
    }

    @Test
    void rejectsOverlappingReservation() throws Exception {
        jdbcTemplate.execute("""
                insert into customers (id, organization_id, name, email, phone, created_at, updated_at)
                values (1, 1, 'Existing User', 'existing@example.com', '010-0000-0000', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    1, 1, 1, 12, 1, 'RSV-20260615-ABCD', 'CONFIRMED',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                    2, 36000.00, 0, 0, 36000.00,
                    current_timestamp, current_timestamp
                )
                """);

        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-06-15",
                  "startTime": "11:00",
                  "durationMinutes": 60,
                  "guestCount": 2,
                  "customer": {
                    "name": "Kim Minji",
                    "email": "minji@example.com",
                    "phone": "010-1234-5678"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/public/reservations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_SLOT_UNAVAILABLE"));
    }

    @Test
    void rejectsReservationOnClosedDay() throws Exception {
        // 2026-06-13 is a Saturday (ISO 6) — no availability rule for day 6
        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-06-13",
                  "startTime": "10:00",
                  "durationMinutes": 60,
                  "guestCount": 2,
                  "customer": {
                    "name": "Test User",
                    "email": "test@example.com",
                    "phone": "010-0000-0000"
                  }
                }
                """;
        mockMvc.perform(post("/api/v1/public/reservations")
                        .contentType("application/json").content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESOURCE_UNAVAILABLE_ON_DAY"));
    }
}
