package com.jean325.slotops.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class AvailabilityRuleRepositoryTest {

    @Autowired
    private AvailabilityRuleRepository ruleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsRulesForResource() {
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1, 'Org', 'org', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into locations (id, organization_id, name, timezone, status, created_at, updated_at)
                values (1, 1, 'Loc', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    1, 1, 1, 'ROOM', 'Room A', 4, 60, 60, 180, 0,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into availability_rules (
                    resource_id, day_of_week, start_time, end_time, is_open,
                    lead_time_minutes, created_at, updated_at
                ) values (1, 1, '09:00', '18:00', true, 60, current_timestamp, current_timestamp)
                """);

        List<AvailabilityRule> rules = ruleRepository.findByResourceId(1L);

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getDayOfWeek()).isEqualTo(1);
        assertThat(rules.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(rules.get(0).isOpen()).isTrue();
    }
}
