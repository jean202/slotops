package com.jean325.slotops.reservation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PublicReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from reservations");
        jdbcTemplate.execute("delete from customers");
        jdbcTemplate.execute("delete from resources");
        jdbcTemplate.execute("delete from locations");
        jdbcTemplate.execute("delete from organizations");

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
    }

    @Test
    void createsReservation() throws Exception {
        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-05-10",
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
                    1, 1, 1, 12, 1, 'RSV-20260510-ABCD', 'CONFIRMED',
                    TIMESTAMP WITH TIME ZONE '2026-05-10 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-05-10 12:00:00+09:00',
                    2, 36000.00, 0, 0, 36000.00,
                    current_timestamp, current_timestamp
                )
                """);

        String requestBody = """
                {
                  "resourceId": 12,
                  "date": "2026-05-10",
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
}
