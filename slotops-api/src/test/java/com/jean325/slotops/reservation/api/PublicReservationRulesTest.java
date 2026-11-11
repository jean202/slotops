package com.jean325.slotops.reservation.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Booking rules a customer hits on the public reservation endpoint: resource
 * limits, business hours, lead time, same-day cutoff, blackouts and buffers.
 * The clock is frozen at 2026-05-01 09:00 KST (a Friday).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@Sql("/sql/clean.sql")
class PublicReservationRulesTest {

    private static final String MONDAY = "2026-06-15";
    private static final String TODAY_FRIDAY = "2026-05-01";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

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
        // capacity 4, 60-minute slots, 60..180 minute bookings, 15-minute buffer
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
        addRule(1, "09:00", "18:00", 0, null);
    }

    private void addRule(int dayOfWeek, String start, String end, int leadTimeMinutes, String sameDayCutoff) {
        jdbcTemplate.update("""
                insert into availability_rules (
                    resource_id, day_of_week, start_time, end_time, is_open,
                    lead_time_minutes, same_day_cutoff_time, created_at, updated_at
                ) values (12, ?, cast(? as time), cast(? as time), true, ?, cast(? as time),
                          current_timestamp, current_timestamp)
                """, dayOfWeek, start, end, leadTimeMinutes, sameDayCutoff);
    }

    private ResultActions book(String date, String startTime, int durationMinutes, int guestCount) throws Exception {
        return book(date, startTime, durationMinutes, guestCount, "Kim Minji", "\"minji@example.com\"", "010-1234-5678");
    }

    private ResultActions book(String date, String startTime, int durationMinutes, int guestCount,
                               String name, String emailJson, String phone) throws Exception {
        String body = """
                {
                  "resourceId": 12, "date": "%s", "startTime": "%s", "durationMinutes": %d, "guestCount": %d,
                  "customer": { "name": "%s", "email": %s, "phone": "%s" }
                }
                """.formatted(date, startTime, durationMinutes, guestCount, name, emailJson, phone);
        return mockMvc.perform(post("/api/v1/public/reservations").contentType("application/json").content(body));
    }

    private int reservationCount() {
        return jdbcTemplate.queryForObject("select count(*) from reservations", Integer.class);
    }

    private void expectRejected(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
        assertThat(reservationCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
    }

    // --- resource limits ---

    @Test
    void rejectsDurationLongerThanResourceMaximum() throws Exception {
        expectRejected(book(MONDAY, "10:00", 240, 2), 400, "RESERVATION_DURATION_OUT_OF_RANGE");
    }

    @Test
    void rejectsDurationNotAlignedToSlotUnit() throws Exception {
        expectRejected(book(MONDAY, "10:00", 90, 2), 400, "RESERVATION_INVALID_SLOT_UNIT");
    }

    @Test
    void rejectsGuestCountAboveCapacity() throws Exception {
        expectRejected(book(MONDAY, "10:00", 60, 5), 400, "RESERVATION_CAPACITY_EXCEEDED");
    }

    @Test
    void rejectsBookingOfInactiveResource() throws Exception {
        jdbcTemplate.update("update resources set status = 'INACTIVE' where id = 12");

        expectRejected(book(MONDAY, "10:00", 60, 2), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void rejectsRequestMissingRequiredFields() throws Exception {
        expectRejected(book(MONDAY, "10:00", 60, 0), 400, "VALIDATION_ERROR");
        expectRejected(book(MONDAY, "10:00", 60, 2, "Kim Minji", "\"minji@example.com\"", ""),
                400, "VALIDATION_ERROR");
    }

    // --- business hours ---

    @Test
    void rejectsBookingStartingBeforeOpening() throws Exception {
        expectRejected(book(MONDAY, "08:00", 60, 2), 400, "RESERVATION_OUTSIDE_BUSINESS_HOURS");
    }

    @Test
    void rejectsBookingEndingAfterClosing() throws Exception {
        expectRejected(book(MONDAY, "17:00", 120, 2), 400, "RESERVATION_OUTSIDE_BUSINESS_HOURS");
    }

    @Test
    void acceptsBookingEndingExactlyAtClosing() throws Exception {
        book(MONDAY, "17:00", 60, 2).andExpect(status().isCreated());
    }

    // --- time-based rules (now = Friday 2026-05-01 09:00 KST) ---

    @Test
    void rejectsBookingInThePast() throws Exception {
        addRule(4, "09:00", "18:00", 0, null);

        expectRejected(book("2026-04-30", "10:00", 60, 2), 400, "RESERVATION_LEAD_TIME_VIOLATION");
    }

    @Test
    void enforcesLeadTime() throws Exception {
        addRule(5, "09:00", "22:00", 120, null);

        expectRejected(book(TODAY_FRIDAY, "10:00", 60, 2), 400, "RESERVATION_LEAD_TIME_VIOLATION");

        // exactly 120 minutes ahead is allowed
        book(TODAY_FRIDAY, "11:00", 60, 2).andExpect(status().isCreated());
    }

    @Test
    void rejectsSameDayBookingAfterCutoff() throws Exception {
        addRule(5, "09:00", "22:00", 0, "09:00");

        expectRejected(book(TODAY_FRIDAY, "15:00", 60, 2), 400, "RESERVATION_PAST_SAME_DAY_CUTOFF");
    }

    @Test
    void allowsSameDayBookingBeforeCutoff() throws Exception {
        addRule(5, "09:00", "22:00", 0, "12:00");

        book(TODAY_FRIDAY, "15:00", 60, 2).andExpect(status().isCreated());
    }

    @Test
    void sameDayCutoffDoesNotAffectFutureDates() throws Exception {
        jdbcTemplate.update("update availability_rules set same_day_cutoff_time = cast('00:01' as time)");

        book(MONDAY, "10:00", 60, 2).andExpect(status().isCreated());
    }

    // --- blackouts and buffers ---

    @Test
    void rejectsBookingOverlappingBlackout() throws Exception {
        jdbcTemplate.execute("""
                insert into blackout_periods (resource_id, start_at, end_at, reason, created_at)
                values (12, TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                        TIMESTAMP WITH TIME ZONE '2026-06-15 14:00:00+09:00', 'Maintenance', current_timestamp)
                """);

        expectRejected(book(MONDAY, "11:00", 120, 2), 400, "RESOURCE_BLACKED_OUT");
    }

    @Test
    void allowsBookingAdjacentToBlackout() throws Exception {
        jdbcTemplate.execute("""
                insert into blackout_periods (resource_id, start_at, end_at, reason, created_at)
                values (12, TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                        TIMESTAMP WITH TIME ZONE '2026-06-15 14:00:00+09:00', 'Maintenance', current_timestamp)
                """);

        book(MONDAY, "10:00", 120, 2).andExpect(status().isCreated());
    }

    @Test
    void bufferBlocksBackToBackBookingButNotTheNextSlot() throws Exception {
        book(MONDAY, "10:00", 120, 2).andExpect(status().isCreated());

        // 15-minute buffer after a booking ending at 12:00
        book(MONDAY, "12:00", 60, 2, "Lee", "\"lee@example.com\"", "010-9999-0000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_SLOT_UNAVAILABLE"));
        book(MONDAY, "13:00", 60, 2, "Lee", "\"lee@example.com\"", "010-9999-0000")
                .andExpect(status().isCreated());

        assertThat(reservationCount()).isEqualTo(2);
    }

    @Test
    void cancelledReservationFreesItsSlot() throws Exception {
        book(MONDAY, "10:00", 120, 2).andExpect(status().isCreated());
        jdbcTemplate.update("update reservations set status = 'CANCELLED'");

        book(MONDAY, "10:00", 120, 2, "Lee", "\"lee@example.com\"", "010-9999-0000")
                .andExpect(status().isCreated());
    }

    // --- pricing, customers, notifications ---

    @Test
    void pricesBookingPerSlotUnit() throws Exception {
        book(MONDAY, "10:00", 180, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pricing.baseAmount").value(54000.0))
                .andExpect(jsonPath("$.pricing.addonAmount").value(0))
                .andExpect(jsonPath("$.pricing.totalAmount").value(54000.0));
    }

    @Test
    void reusesCustomerWithSamePhoneAndUpdatesContact() throws Exception {
        book(MONDAY, "10:00", 60, 2, "Kim Minji", "\"minji@example.com\"", "010-1234-5678")
                .andExpect(status().isCreated());
        book(MONDAY, "14:00", 60, 2, "Kim Min-ji", "\"new@example.com\"", "010-1234-5678")
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("select count(*) from customers", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select name from customers", String.class)).isEqualTo("Kim Min-ji");
        assertThat(jdbcTemplate.queryForObject("select email from customers", String.class))
                .isEqualTo("new@example.com");
        assertThat(jdbcTemplate.queryForObject("select count(distinct customer_id) from reservations", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void notifiesByEmailWhenCustomerHasEmail() throws Exception {
        book(MONDAY, "10:00", 60, 2).andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("select channel from notification_logs", String.class))
                .isEqualTo("EMAIL");
        assertThat(jdbcTemplate.queryForObject("select recipient from notification_logs", String.class))
                .isEqualTo("minji@example.com");
    }

    @Test
    void notifiesBySmsWhenCustomerHasNoEmail() throws Exception {
        book(MONDAY, "10:00", 60, 2, "Kim Minji", "null", "010-1234-5678").andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("select channel from notification_logs", String.class))
                .isEqualTo("SMS");
        assertThat(jdbcTemplate.queryForObject("select recipient from notification_logs", String.class))
                .isEqualTo("010-1234-5678");
        assertThat(jdbcTemplate.queryForObject("select delivery_status from notification_logs", String.class))
                .isEqualTo("PENDING");
    }
}
