# Availability & Booking Logic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement weekly availability rules, blackout periods, slot generation, and conflict detection — with a public API for querying available time slots and enforcement on reservation creation.

**Architecture:** `SlotCalculator` and `ReservationConflictChecker` are pure POJOs (no Spring/JPA) that receive data as parameters and are injected as `@Component`s. `AvailabilityService` orchestrates repository queries and calls these domain services. `ReservationService` gains availability validation via `AvailabilityService.validateAvailability()` before accepting reservations.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Java 17 records, H2 for integration tests, JUnit 5, MockMvc

---

## File Map

**Create:**
- `availability/domain/AvailabilityRule.java` — JPA entity
- `availability/domain/BlackoutPeriod.java` — JPA entity
- `availability/domain/AvailabilityRuleRepository.java`
- `availability/domain/BlackoutPeriodRepository.java`
- `availability/domain/ReservationConflictChecker.java` — `@Component`, no repos
- `availability/domain/SlotCalculator.java` — `@Component`, no repos
- `availability/application/AvailabilityService.java`
- `availability/api/PublicAvailabilityController.java`
- `availability/api/AvailabilityController.java`
- `availability/dto/CreateAvailabilityRuleRequest.java`
- `availability/dto/AvailabilityRuleResponse.java`
- `availability/dto/CreateBlackoutPeriodRequest.java`
- `availability/dto/BlackoutPeriodResponse.java`
- `availability/dto/AvailableSlotsResponse.java`
- `test/.../availability/domain/ReservationConflictCheckerTest.java`
- `test/.../availability/domain/SlotCalculatorTest.java`
- `test/.../availability/api/PublicAvailabilityControllerTest.java`
- `test/.../availability/api/AvailabilityControllerTest.java`

**Modify:**
- `reservation/domain/ReservationRepository.java` — add `findActiveForResourceBetween`
- `reservation/application/ReservationService.java` — use Location timezone + call `validateAvailability`
- `test/.../reservation/api/PublicReservationControllerTest.java` — add availability_rules inserts

---

## Task 1: AvailabilityRule and BlackoutPeriod entities + repositories

**Files:**
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/AvailabilityRule.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/BlackoutPeriod.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/AvailabilityRuleRepository.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/BlackoutPeriodRepository.java`
- Test: `slotops-api/src/test/java/com/jean325/slotops/availability/domain/AvailabilityRuleRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.AvailabilityRuleRepositoryTest" 2>&1 | tail -10
```
Expected: compilation error — `AvailabilityRule`, `AvailabilityRuleRepository` not found.

- [ ] **Step 3: Create AvailabilityRule entity**

```java
package com.jean325.slotops.availability.domain;

import com.jean325.slotops.global.common.BaseEntity;
import com.jean325.slotops.resource.domain.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isOpen;
    private int leadTimeMinutes;
    private LocalTime sameDayCutoffTime;

    protected AvailabilityRule() {}

    public AvailabilityRule(Resource resource, int dayOfWeek, LocalTime startTime, LocalTime endTime,
                             boolean isOpen, int leadTimeMinutes, LocalTime sameDayCutoffTime) {
        this.resource = resource;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpen = isOpen;
        this.leadTimeMinutes = leadTimeMinutes;
        this.sameDayCutoffTime = sameDayCutoffTime;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isOpen() { return isOpen; }
    public int getLeadTimeMinutes() { return leadTimeMinutes; }
    public LocalTime getSameDayCutoffTime() { return sameDayCutoffTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setOpen(boolean open) { this.isOpen = open; }
    public void setLeadTimeMinutes(int leadTimeMinutes) { this.leadTimeMinutes = leadTimeMinutes; }
    public void setSameDayCutoffTime(LocalTime sameDayCutoffTime) { this.sameDayCutoffTime = sameDayCutoffTime; }
}
```

- [ ] **Step 4: Create BlackoutPeriod entity**

```java
package com.jean325.slotops.availability.domain;

import com.jean325.slotops.resource.domain.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "blackout_periods")
public class BlackoutPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String reason;

    @CreatedDate
    private Instant createdAt;

    protected BlackoutPeriod() {}

    public BlackoutPeriod(Resource resource, OffsetDateTime startAt, OffsetDateTime endAt, String reason) {
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Create repositories**

```java
package com.jean325.slotops.availability.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    List<AvailabilityRule> findByResourceId(Long resourceId);
    Optional<AvailabilityRule> findByResourceIdAndDayOfWeek(Long resourceId, int dayOfWeek);
}
```

```java
package com.jean325.slotops.availability.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlackoutPeriodRepository extends JpaRepository<BlackoutPeriod, Long> {

    @Query("""
            select b from BlackoutPeriod b
            where b.resource.id = :resourceId
              and b.startAt < :windowEnd
              and b.endAt > :windowStart
            """)
    List<BlackoutPeriod> findOverlappingBetween(
            @Param("resourceId") Long resourceId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    @Query("""
            select case when count(b) > 0 then true else false end
            from BlackoutPeriod b
            where b.resource.id = :resourceId
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlap(
            @Param("resourceId") Long resourceId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    List<BlackoutPeriod> findByResourceId(Long resourceId);
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.AvailabilityRuleRepositoryTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/availability/domain/ \
        slotops-api/src/test/java/com/jean325/slotops/availability/domain/AvailabilityRuleRepositoryTest.java
git commit -m "feat: add AvailabilityRule and BlackoutPeriod entities and repositories"
```

---

## Task 2: ReservationConflictChecker

**Files:**
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/ReservationConflictChecker.java`
- Test: `slotops-api/src/test/java/com/jean325/slotops/availability/domain/ReservationConflictCheckerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.jean325.slotops.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean325.slotops.reservation.domain.Reservation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationConflictCheckerTest {

    private ReservationConflictChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ReservationConflictChecker();
    }

    private Reservation stubReservation(String start, String end) {
        return new Reservation(
                1L, 1L, null, null, "CODE", null,
                odt(start), odt(end), 1,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null
        );
    }

    private OffsetDateTime odt(String time) {
        return OffsetDateTime.parse("2026-05-19T" + time + ":00+09:00");
    }

    @Test
    void returnsFalseWhenNoReservations() {
        assertThat(checker.hasConflict(List.of(), odt("10:00"), odt("11:00"), 0)).isFalse();
    }

    @Test
    void returnsFalseWhenAdjacentNoBuffer() {
        // existing: 09:00-10:00, requested: 10:00-11:00 — touching but not overlapping
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:00"), odt("11:00"), 0
        )).isFalse();
    }

    @Test
    void returnsTrueWhenOverlapping() {
        // existing: 09:00-11:00, requested: 10:00-12:00 — overlap at 10-11
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "11:00")),
                odt("10:00"), odt("12:00"), 0
        )).isTrue();
    }

    @Test
    void returnsTrueWhenAdjacentWithBuffer() {
        // existing: 09:00-10:00, buffer=15 → window: 09:45-10:15
        // requested: 10:00-11:00 → start 10:00 is inside window end 10:15
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:00"), odt("11:00"), 15
        )).isTrue();
    }

    @Test
    void returnsFalseWhenOutsideBuffer() {
        // existing: 09:00-10:00, buffer=15 → existing window end: 10:15
        // requested: 10:30-11:30 — no conflict
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:30"), odt("11:30"), 15
        )).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.ReservationConflictCheckerTest" 2>&1 | tail -10
```
Expected: compilation error — `ReservationConflictChecker` not found. (Note: the `Reservation` stub constructor may also fail — if `Reservation` lacks a matching constructor, adjust the stub to use whatever constructor exists, or add a package-private test constructor.)

- [ ] **Step 3: Check Reservation constructor and adjust stub if needed**

```bash
grep -n "public Reservation" slotops-api/src/main/java/com/jean325/slotops/reservation/domain/Reservation.java
```

If `Reservation` has no suitable constructor for test stubs, add a package-private constructor **only** to `Reservation.java`:

```java
// test-only constructor
Reservation(Long organizationId, Long locationId, Resource resource, Customer customer,
             String reservationCode, ReservationStatus status,
             OffsetDateTime startAt, OffsetDateTime endAt, int guestCount,
             java.math.BigDecimal baseAmount, java.math.BigDecimal addonAmount,
             java.math.BigDecimal discountAmount, java.math.BigDecimal totalAmount,
             String specialRequest) {
    this.startAt = startAt;
    this.endAt = endAt;
}
```

Then update the `stubReservation` helper in the test to match the actual constructor signature.

- [ ] **Step 4: Implement ReservationConflictChecker**

```java
package com.jean325.slotops.availability.domain;

import com.jean325.slotops.reservation.domain.Reservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReservationConflictChecker {

    public boolean hasConflict(
            List<Reservation> reservations,
            OffsetDateTime requestedStart,
            OffsetDateTime requestedEnd,
            int bufferMinutes
    ) {
        OffsetDateTime windowStart = requestedStart.minusMinutes(bufferMinutes);
        OffsetDateTime windowEnd = requestedEnd.plusMinutes(bufferMinutes);
        return reservations.stream().anyMatch(r ->
                r.getStartAt().isBefore(windowEnd) && r.getEndAt().isAfter(windowStart)
        );
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.ReservationConflictCheckerTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/availability/domain/ReservationConflictChecker.java \
        slotops-api/src/test/java/com/jean325/slotops/availability/domain/ReservationConflictCheckerTest.java
git commit -m "feat: add ReservationConflictChecker domain service"
```

---

## Task 3: SlotCalculator

**Files:**
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/domain/SlotCalculator.java`
- Test: `slotops-api/src/test/java/com/jean325/slotops/availability/domain/SlotCalculatorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.jean325.slotops.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.resource.domain.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlotCalculatorTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate MONDAY = LocalDate.of(2026, 5, 18); // ISO dayOfWeek = 1

    private SlotCalculator calculator;
    private Resource resource;
    private AvailabilityRule openRule;
    // "now" far in the past so lead time never triggers
    private OffsetDateTime pastNow;

    @BeforeEach
    void setUp() {
        calculator = new SlotCalculator(new ReservationConflictChecker());
        resource = stubResource(60, 60, 180, 0);
        openRule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, null);
        pastNow = OffsetDateTime.parse("2026-01-01T00:00:00+09:00");
    }

    @Test
    void returnsSlotsCoveringBusinessHours() {
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        // 09:00-18:00 with 60-min slots = 9 slots
        assertThat(slots).hasSize(9);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(8).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void returnsEmptyWhenNoRuleForDay() {
        AvailabilityRule tuesdayRule = new AvailabilityRule(null, 2, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(tuesdayRule), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void returnsEmptyWhenDayIsClosedByRule() {
        AvailabilityRule closed = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), false, 0, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(closed), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void filtersSlotsCoveredByBlackout() {
        // Blackout covers 10:00-12:00
        BlackoutPeriod blackout = new BlackoutPeriod(null,
                OffsetDateTime.parse("2026-05-18T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-18T12:00:00+09:00"),
                "Maintenance");
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(blackout), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        // 09:00 ok, 10:00 blocked, 11:00 blocked, 12:00 ok ... 17:00 ok = 7 slots
        assertThat(slots).hasSize(7);
        boolean anyBlockedSlot = slots.stream().anyMatch(s ->
                s.startAt().atZoneSameInstant(SEOUL).toLocalTime().equals(LocalTime.of(10, 0)) ||
                s.startAt().atZoneSameInstant(SEOUL).toLocalTime().equals(LocalTime.of(11, 0))
        );
        assertThat(anyBlockedSlot).isFalse();
    }

    @Test
    void filtersSlotsThatViolateLeadTime() {
        // now = 09:30 Seoul, lead = 60 min → slots starting before 10:30 are blocked
        OffsetDateTime now = OffsetDateTime.parse("2026-05-18T09:30:00+09:00");
        AvailabilityRule rule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 60, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(rule), List.of(), List.of(),
                MONDAY, 60, SEOUL, now
        );
        // 09:00 blocked (09:00 - 60 = 08:00 < now 09:30), 10:00 blocked (10:00-60=09:00 < 09:30)
        // 11:00 ok (11:00-60=10:00 > 09:30) ... 17:00 ok = 7 slots
        assertThat(slots).hasSize(7);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void returnsEmptyWhenSameDayCutoffPassed() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-18T10:00:00+09:00");
        AvailabilityRule rule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, LocalTime.of(9, 30));
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(rule), List.of(), List.of(),
                MONDAY, 60, SEOUL, now
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void filtersSlotConflictingWithExistingReservationAndBuffer() {
        resource = stubResource(60, 60, 180, 15);
        // Existing reservation: 10:00-11:00. Buffer=15 → window 09:45-11:15
        // 09:00 slot: end 10:00, with buffer window 08:45-10:15 → overlaps with existing 10:00-11:00
        // 10:00 slot: start 10:00 → directly overlapping
        // 11:00 slot: start 11:00, window 10:45-12:00 → overlaps with existing end 11:00
        // 12:00 slot: start 12:00, window 11:45-13:00 → overlaps with existing window 09:45-11:15? No
        // Wait: buffer is applied to the *existing* reservation, not the requested slot
        // The conflict check: windowStart = requestedStart - buffer, windowEnd = requestedEnd + buffer
        // vs existing reservation startAt < windowEnd && endAt > windowStart
        // 09:00 requested (09:00-10:00), windowStart=08:45, windowEnd=10:15
        //   existing 10:00-11:00: startAt(10:00) < windowEnd(10:15) AND endAt(11:00) > windowStart(08:45) → CONFLICT
        // 11:00 requested (11:00-12:00), windowStart=10:45, windowEnd=12:15
        //   existing 10:00-11:00: startAt(10:00) < windowEnd(12:15) AND endAt(11:00) > windowStart(10:45) → CONFLICT
        // So 09:00, 10:00, 11:00 blocked. 12:00-17:00 = 6 slots.
        Reservation existing = stubReservation(
                OffsetDateTime.parse("2026-05-18T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-18T11:00:00+09:00")
        );
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(), List.of(existing),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).hasSize(6);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(12, 0));
    }

    private Resource stubResource(int slotUnit, int minBooking, int maxBooking, int buffer) {
        return new Resource(null, null, null, "Test", 4, slotUnit, minBooking, maxBooking, buffer,
                BigDecimal.valueOf(10000), "KRW");
    }

    private Reservation stubReservation(OffsetDateTime start, OffsetDateTime end) {
        // Uses the minimal constructor added in Task 2 Step 3
        return new Reservation(start, end);
    }
}
```

> **Note on `stubReservation`:** `Reservation` needs a minimal constructor for tests. Add this to `Reservation.java` if not already present:
> ```java
> // test-only constructor
> Reservation(OffsetDateTime startAt, OffsetDateTime endAt) {
>     this.startAt = startAt;
>     this.endAt = endAt;
> }
> ```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.SlotCalculatorTest" 2>&1 | tail -10
```
Expected: compilation error — `SlotCalculator` not found.

- [ ] **Step 3: Implement SlotCalculator**

```java
package com.jean325.slotops.availability.domain;

import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.resource.domain.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlotCalculator {

    private final ReservationConflictChecker conflictChecker;

    public SlotCalculator(ReservationConflictChecker conflictChecker) {
        this.conflictChecker = conflictChecker;
    }

    public record AvailableSlot(OffsetDateTime startAt, OffsetDateTime endAt) {}

    public List<AvailableSlot> calculate(
            Resource resource,
            List<AvailabilityRule> rules,
            List<BlackoutPeriod> blackouts,
            List<Reservation> reservations,
            LocalDate date,
            int durationMinutes,
            ZoneId timezone,
            OffsetDateTime now
    ) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        AvailabilityRule rule = rules.stream()
                .filter(r -> r.getDayOfWeek() == dayOfWeek && r.isOpen())
                .findFirst()
                .orElse(null);
        if (rule == null) return List.of();

        // Same-day cutoff check
        if (date.equals(now.atZoneSameInstant(timezone).toLocalDate())
                && rule.getSameDayCutoffTime() != null
                && !now.atZoneSameInstant(timezone).toLocalTime().isBefore(rule.getSameDayCutoffTime())) {
            return List.of();
        }

        List<AvailableSlot> slots = new ArrayList<>();
        LocalTime cursor = rule.getStartTime();
        LocalTime latestStart = rule.getEndTime().minusMinutes(durationMinutes);

        while (!cursor.isAfter(latestStart)) {
            OffsetDateTime startAt = date.atTime(cursor).atZone(timezone).toOffsetDateTime();
            OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);

            // Lead time check
            if (startAt.minusMinutes(rule.getLeadTimeMinutes()).isBefore(now)) {
                cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
                continue;
            }

            // Blackout check
            boolean blacked = blackouts.stream().anyMatch(b ->
                    b.getStartAt().isBefore(endAt) && b.getEndAt().isAfter(startAt));
            if (blacked) {
                cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
                continue;
            }

            // Conflict check
            if (!conflictChecker.hasConflict(reservations, startAt, endAt, resource.getBufferMinutes())) {
                slots.add(new AvailableSlot(startAt, endAt));
            }

            cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
        }

        return slots;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.domain.SlotCalculatorTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/availability/domain/SlotCalculator.java \
        slotops-api/src/test/java/com/jean325/slotops/availability/domain/SlotCalculatorTest.java
git commit -m "feat: add SlotCalculator domain service"
```

---

## Task 4: DTOs + AvailabilityService

**Files:**
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/dto/CreateAvailabilityRuleRequest.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/dto/AvailabilityRuleResponse.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/dto/CreateBlackoutPeriodRequest.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/dto/BlackoutPeriodResponse.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/dto/AvailableSlotsResponse.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/application/AvailabilityService.java`
- Modify: `slotops-api/src/main/java/com/jean325/slotops/reservation/domain/ReservationRepository.java`

- [ ] **Step 1: Create DTOs**

```java
// CreateAvailabilityRuleRequest.java
package com.jean325.slotops.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CreateAvailabilityRuleRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean isOpen,
        @NotNull Integer leadTimeMinutes,
        LocalTime sameDayCutoffTime
) {}
```

```java
// AvailabilityRuleResponse.java
package com.jean325.slotops.availability.dto;

import java.time.LocalTime;

public record AvailabilityRuleResponse(
        Long id,
        Long resourceId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isOpen,
        int leadTimeMinutes,
        LocalTime sameDayCutoffTime
) {}
```

```java
// CreateBlackoutPeriodRequest.java
package com.jean325.slotops.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateBlackoutPeriodRequest(
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        String reason
) {}
```

```java
// BlackoutPeriodResponse.java
package com.jean325.slotops.availability.dto;

import java.time.OffsetDateTime;

public record BlackoutPeriodResponse(
        Long id,
        Long resourceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String reason
) {}
```

```java
// AvailableSlotsResponse.java
package com.jean325.slotops.availability.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AvailableSlotsResponse(List<Slot> slots) {
    public record Slot(OffsetDateTime startAt, OffsetDateTime endAt) {}
}
```

- [ ] **Step 2: Add `findActiveForResourceBetween` to ReservationRepository**

```java
// Add to ReservationRepository.java
@Query("""
        select r from Reservation r
        where r.resource.id = :resourceId
          and r.status in :activeStatuses
          and r.startAt < :windowEnd
          and r.endAt > :windowStart
        """)
List<Reservation> findActiveForResourceBetween(
        @Param("resourceId") Long resourceId,
        @Param("windowStart") OffsetDateTime windowStart,
        @Param("windowEnd") OffsetDateTime windowEnd,
        @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
);
```

- [ ] **Step 3: Create AvailabilityService**

```java
package com.jean325.slotops.availability.application;

import com.jean325.slotops.availability.domain.AvailabilityRule;
import com.jean325.slotops.availability.domain.AvailabilityRuleRepository;
import com.jean325.slotops.availability.domain.BlackoutPeriod;
import com.jean325.slotops.availability.domain.BlackoutPeriodRepository;
import com.jean325.slotops.availability.domain.SlotCalculator;
import com.jean325.slotops.availability.dto.AvailabilityRuleResponse;
import com.jean325.slotops.availability.dto.AvailableSlotsResponse;
import com.jean325.slotops.availability.dto.BlackoutPeriodResponse;
import com.jean325.slotops.availability.dto.CreateAvailabilityRuleRequest;
import com.jean325.slotops.availability.dto.CreateBlackoutPeriodRequest;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceRepository;
import com.jean325.slotops.resource.domain.ResourceStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AvailabilityService {

    private final AvailabilityRuleRepository ruleRepository;
    private final BlackoutPeriodRepository blackoutRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final SlotCalculator slotCalculator;

    public AvailabilityService(
            AvailabilityRuleRepository ruleRepository,
            BlackoutPeriodRepository blackoutRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            SlotCalculator slotCalculator
    ) {
        this.ruleRepository = ruleRepository;
        this.blackoutRepository = blackoutRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.slotCalculator = slotCalculator;
    }

    // --- Availability Rules ---

    public AvailabilityRuleResponse createRule(Long orgId, Long resourceId, CreateAvailabilityRuleRequest request) {
        Resource resource = getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = new AvailabilityRule(
                resource,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.isOpen(),
                request.leadTimeMinutes(),
                request.sameDayCutoffTime()
        );
        return toRuleResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> listRules(Long orgId, Long resourceId) {
        getResourceForOrg(resourceId, orgId);
        return ruleRepository.findByResourceId(resourceId).stream()
                .map(this::toRuleResponse).toList();
    }

    public AvailabilityRuleResponse updateRule(Long orgId, Long resourceId, Long ruleId,
                                                CreateAvailabilityRuleRequest request) {
        getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("RULE_NOT_FOUND", "Availability rule not found", HttpStatus.NOT_FOUND));
        rule.setStartTime(request.startTime());
        rule.setEndTime(request.endTime());
        rule.setOpen(request.isOpen());
        rule.setLeadTimeMinutes(request.leadTimeMinutes());
        rule.setSameDayCutoffTime(request.sameDayCutoffTime());
        return toRuleResponse(ruleRepository.save(rule));
    }

    public void deleteRule(Long orgId, Long resourceId, Long ruleId) {
        getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("RULE_NOT_FOUND", "Availability rule not found", HttpStatus.NOT_FOUND));
        ruleRepository.delete(rule);
    }

    // --- Blackout Periods ---

    public BlackoutPeriodResponse createBlackout(Long orgId, Long resourceId, CreateBlackoutPeriodRequest request) {
        Resource resource = getResourceForOrg(resourceId, orgId);
        BlackoutPeriod period = new BlackoutPeriod(resource, request.startAt(), request.endAt(), request.reason());
        return toBlackoutResponse(blackoutRepository.save(period));
    }

    @Transactional(readOnly = true)
    public List<BlackoutPeriodResponse> listBlackouts(Long orgId, Long resourceId) {
        getResourceForOrg(resourceId, orgId);
        return blackoutRepository.findByResourceId(resourceId).stream()
                .map(this::toBlackoutResponse).toList();
    }

    public void deleteBlackout(Long orgId, Long resourceId, Long periodId) {
        getResourceForOrg(resourceId, orgId);
        BlackoutPeriod period = blackoutRepository.findById(periodId)
                .filter(p -> p.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("BLACKOUT_NOT_FOUND", "Blackout period not found", HttpStatus.NOT_FOUND));
        blackoutRepository.delete(period);
    }

    // --- Slot Query ---

    @Transactional(readOnly = true)
    public AvailableSlotsResponse getAvailableSlots(Long resourceId, LocalDate date, int durationMinutes) {
        Resource resource = resourceRepository.findByIdAndStatus(resourceId, ResourceStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND));

        if (durationMinutes % resource.getSlotUnitMinutes() != 0) {
            throw new ApiException("INVALID_DURATION",
                    "Duration must be a multiple of the resource's slot unit (" + resource.getSlotUnitMinutes() + " min)",
                    HttpStatus.BAD_REQUEST);
        }

        ZoneId timezone = ZoneId.of(resource.getLocation().getTimezone());
        ZonedDateTime dayStart = date.atStartOfDay(timezone);
        ZonedDateTime dayEnd = date.plusDays(1).atStartOfDay(timezone);

        List<AvailabilityRule> rules = ruleRepository.findByResourceId(resourceId);
        List<BlackoutPeriod> blackouts = blackoutRepository.findOverlappingBetween(
                resourceId, dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime());
        List<Reservation> reservations = reservationRepository.findActiveForResourceBetween(
                resourceId, dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        List<SlotCalculator.AvailableSlot> slots = slotCalculator.calculate(
                resource, rules, blackouts, reservations, date, durationMinutes, timezone, OffsetDateTime.now());

        return new AvailableSlotsResponse(
                slots.stream().map(s -> new AvailableSlotsResponse.Slot(s.startAt(), s.endAt())).toList()
        );
    }

    // --- Availability Validation (called by ReservationService) ---

    @Transactional(readOnly = true)
    public void validateAvailability(Resource resource, OffsetDateTime startAt, OffsetDateTime endAt) {
        ZoneId timezone = ZoneId.of(resource.getLocation().getTimezone());
        LocalDate date = startAt.atZoneSameInstant(timezone).toLocalDate();
        int dayOfWeek = date.getDayOfWeek().getValue();

        AvailabilityRule rule = ruleRepository.findByResourceIdAndDayOfWeek(resource.getId(), dayOfWeek)
                .orElseThrow(() -> new ApiException(
                        "RESOURCE_UNAVAILABLE_ON_DAY",
                        "Resource is not available on this day of the week.",
                        HttpStatus.BAD_REQUEST));

        if (!rule.isOpen()) {
            throw new ApiException("RESOURCE_UNAVAILABLE_ON_DAY",
                    "Resource is not available on this day of the week.", HttpStatus.BAD_REQUEST);
        }

        java.time.LocalTime startLocal = startAt.atZoneSameInstant(timezone).toLocalTime();
        java.time.LocalTime endLocal = endAt.atZoneSameInstant(timezone).toLocalTime();
        if (startLocal.isBefore(rule.getStartTime()) || endLocal.isAfter(rule.getEndTime())) {
            throw new ApiException("RESERVATION_OUTSIDE_BUSINESS_HOURS",
                    "Reservation is outside the resource's business hours.", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (startAt.minusMinutes(rule.getLeadTimeMinutes()).isBefore(now)) {
            throw new ApiException("RESERVATION_LEAD_TIME_VIOLATION",
                    "Reservation does not meet the required lead time.", HttpStatus.BAD_REQUEST);
        }

        if (date.equals(now.atZoneSameInstant(timezone).toLocalDate())
                && rule.getSameDayCutoffTime() != null
                && !now.atZoneSameInstant(timezone).toLocalTime().isBefore(rule.getSameDayCutoffTime())) {
            throw new ApiException("RESERVATION_PAST_SAME_DAY_CUTOFF",
                    "Same-day booking cutoff time has passed.", HttpStatus.BAD_REQUEST);
        }

        if (blackoutRepository.existsOverlap(resource.getId(), startAt, endAt)) {
            throw new ApiException("RESOURCE_BLACKED_OUT",
                    "Resource is blacked out during the requested time.", HttpStatus.BAD_REQUEST);
        }
    }

    private Resource getResourceForOrg(Long resourceId, Long orgId) {
        return resourceRepository.findById(resourceId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ApiException("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND));
    }

    private AvailabilityRuleResponse toRuleResponse(AvailabilityRule rule) {
        return new AvailabilityRuleResponse(
                rule.getId(), rule.getResource().getId(),
                rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime(),
                rule.isOpen(), rule.getLeadTimeMinutes(), rule.getSameDayCutoffTime()
        );
    }

    private BlackoutPeriodResponse toBlackoutResponse(BlackoutPeriod period) {
        return new BlackoutPeriodResponse(
                period.getId(), period.getResource().getId(),
                period.getStartAt(), period.getEndAt(), period.getReason()
        );
    }
}
```

- [ ] **Step 4: Build to verify compilation**

```bash
cd slotops-api && ./gradlew compileJava 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/availability/ \
        slotops-api/src/main/java/com/jean325/slotops/reservation/domain/ReservationRepository.java
git commit -m "feat: add AvailabilityService with slot query and availability validation"
```

---

## Task 5: Controllers

**Files:**
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/api/PublicAvailabilityController.java`
- Create: `slotops-api/src/main/java/com/jean325/slotops/availability/api/AvailabilityController.java`
- Test: `slotops-api/src/test/java/com/jean325/slotops/availability/api/PublicAvailabilityControllerTest.java`
- Test: `slotops-api/src/test/java/com/jean325/slotops/availability/api/AvailabilityControllerTest.java`

- [ ] **Step 1: Write integration test for public slot query**

```java
package com.jean325.slotops.availability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class PublicAvailabilityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from availability_rules");
        jdbcTemplate.execute("delete from blackout_periods");
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
        // Monday = day_of_week 1 (ISO). 2026-05-18 is a Monday.
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
                        .param("date", "2026-05-18")
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(9));
    }

    @Test
    void returnsEmptyForClosedDay() throws Exception {
        // Sunday (ISO 7) — no rule → empty
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", "2026-05-17")
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    void returns400ForInvalidDuration() throws Exception {
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", "2026-05-18")
                        .param("duration", "45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DURATION"));
    }

    @Test
    void blocksBlackedOutDay() throws Exception {
        jdbcTemplate.execute("""
                insert into blackout_periods (resource_id, start_at, end_at, reason, created_at)
                values (1,
                    TIMESTAMP WITH TIME ZONE '2026-05-18 00:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-05-18 23:59:00+09:00',
                    'Closed', current_timestamp)
                """);
        mockMvc.perform(get("/api/v1/public/resources/1/available-slots")
                        .param("date", "2026-05-18")
                        .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.api.PublicAvailabilityControllerTest" 2>&1 | tail -10
```
Expected: compilation error or 404 — controller not yet created.

- [ ] **Step 3: Create PublicAvailabilityController**

```java
package com.jean325.slotops.availability.api;

import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.availability.dto.AvailableSlotsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/resources")
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    public PublicAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{resourceId}/available-slots")
    public AvailableSlotsResponse getAvailableSlots(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int duration
    ) {
        return availabilityService.getAvailableSlots(resourceId, date, duration);
    }
}
```

Also update `SecurityConfig.java` to permit the new public GET path:

```java
// Change this line:
.requestMatchers(HttpMethod.GET, "/public/**").permitAll()
// To:
.requestMatchers(HttpMethod.GET, "/public/**", "/api/v1/public/**").permitAll()
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.api.PublicAvailabilityControllerTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Write admin CRUD test**

```java
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from availability_rules");
        jdbcTemplate.execute("delete from blackout_periods");
        jdbcTemplate.execute("delete from resources");
        jdbcTemplate.execute("delete from locations");
        jdbcTemplate.execute("delete from organizations");

        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1, 'Org', 'org', 'ACTIVE', current_timestamp, current_timestamp)
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
        mockMvc.perform(get("/api/v1/organizations/1/resources/1/availability-rules"))
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
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reason").value("Holiday"));
    }
}
```

- [ ] **Step 6: Create AvailabilityController**

```java
package com.jean325.slotops.availability.api;

import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.availability.dto.AvailabilityRuleResponse;
import com.jean325.slotops.availability.dto.BlackoutPeriodResponse;
import com.jean325.slotops.availability.dto.CreateAvailabilityRuleRequest;
import com.jean325.slotops.availability.dto.CreateBlackoutPeriodRequest;
import com.jean325.slotops.global.config.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/resources/{resourceId}")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/availability-rules")
    public ResponseEntity<ApiResponse<AvailabilityRuleResponse>> createRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateAvailabilityRuleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createRule(orgId, resourceId, request)));
    }

    @GetMapping("/availability-rules")
    public ResponseEntity<ApiResponse<List<AvailabilityRuleResponse>>> listRules(
            @PathVariable Long orgId,
            @PathVariable Long resourceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listRules(orgId, resourceId)));
    }

    @PutMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<AvailabilityRuleResponse>> updateRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId,
            @Valid @RequestBody CreateAvailabilityRuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.updateRule(orgId, resourceId, ruleId, request)));
    }

    @DeleteMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId
    ) {
        availabilityService.deleteRule(orgId, resourceId, ruleId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<BlackoutPeriodResponse>> createBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateBlackoutPeriodRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createBlackout(orgId, resourceId, request)));
    }

    @GetMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<List<BlackoutPeriodResponse>>> listBlackouts(
            @PathVariable Long orgId,
            @PathVariable Long resourceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listBlackouts(orgId, resourceId)));
    }

    @DeleteMapping("/blackout-periods/{periodId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long periodId
    ) {
        availabilityService.deleteBlackout(orgId, resourceId, periodId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

- [ ] **Step 7: Run all availability tests**

```bash
cd slotops-api && ./gradlew test --tests "com.jean325.slotops.availability.*" 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/availability/api/ \
        slotops-api/src/main/java/com/jean325/slotops/global/config/SecurityConfig.java \
        slotops-api/src/test/java/com/jean325/slotops/availability/api/
git commit -m "feat: add availability and public slot query controllers"
```

---

## Task 6: Update ReservationService — timezone fix + availability validation

**Files:**
- Modify: `slotops-api/src/main/java/com/jean325/slotops/reservation/application/ReservationService.java`
- Modify: `slotops-api/src/test/java/com/jean325/slotops/reservation/api/PublicReservationControllerTest.java`

- [ ] **Step 1: Update existing reservation tests to insert availability_rules**

In `PublicReservationControllerTest`, in the `setUp()` method, add after the resource insert:

```java
// 2026-05-10 is a Sunday (ISO 7)
jdbcTemplate.execute("""
        insert into availability_rules (
            resource_id, day_of_week, start_time, end_time, is_open,
            lead_time_minutes, created_at, updated_at
        ) values (12, 7, '09:00', '18:00', true, 0, current_timestamp, current_timestamp)
        """);
```

Also add to `@BeforeEach` cleanup:
```java
jdbcTemplate.execute("delete from availability_rules");
```

- [ ] **Step 2: Update ReservationService**

In `ReservationService.java`:

1. Add `AvailabilityService` constructor parameter and field:
```java
private final AvailabilityService availabilityService;

// Add to constructor:
this.availabilityService = availabilityService;
```

2. Replace the timezone line in `createPublicReservation`:
```java
// Replace:
ZoneId.of("Asia/Seoul").getRules().getOffset(request.date().atTime(request.startTime()))
// With:
ZoneId.of(resource.getLocation().getTimezone()).getRules().getOffset(request.date().atTime(request.startTime()))
```

3. Add availability validation call after the existing `reservationPolicyValidator.validate()` call:
```java
reservationPolicyValidator.validate(resource, startAt, endAt, request.guestCount());
availabilityService.validateAvailability(resource, startAt, endAt);
```

- [ ] **Step 3: Run all tests**

```bash
cd slotops-api && ./gradlew test 2>&1 | tail -15
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Add a test verifying availability rejection**

Add to `PublicReservationControllerTest`:

```java
@Test
void rejectsReservationOnClosedDay() throws Exception {
    // Saturday (ISO 6) — no availability rule
    String requestBody = """
            {
              "resourceId": 12,
              "date": "2026-05-09",
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
```

- [ ] **Step 5: Run tests again to confirm new test passes**

```bash
cd slotops-api && ./gradlew test 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add slotops-api/src/main/java/com/jean325/slotops/reservation/application/ReservationService.java \
        slotops-api/src/test/java/com/jean325/slotops/reservation/api/PublicReservationControllerTest.java
git commit -m "feat: enforce availability rules on reservation creation and fix timezone"
```

---

## Final Verification

- [ ] **Run full test suite**

```bash
cd slotops-api && ./gradlew test 2>&1 | grep -E "BUILD|tests|failures|errors" | tail -10
```
Expected: `BUILD SUCCESSFUL`, 0 failures, 0 errors.

- [ ] **Final commit if any stragglers**

```bash
git status
# If clean, nothing to do. Otherwise:
git add -A && git commit -m "chore: Phase 3 cleanup"
```
