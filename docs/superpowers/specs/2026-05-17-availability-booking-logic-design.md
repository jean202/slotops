# Phase 3: Availability & Booking Logic — Design Spec

**Date**: 2026-05-17  
**Status**: Approved

---

## Overview

Implement the availability module for SlotOps: weekly availability rules, blackout periods, slot generation, and conflict detection. Expose a public API for querying available time slots, and enforce availability constraints during reservation creation.

---

## Module Structure

```
availability/
  domain/
    AvailabilityRule.java              # entity
    BlackoutPeriod.java                # entity
    AvailabilityRuleRepository.java
    BlackoutPeriodRepository.java
    SlotCalculator.java                # pure domain service (POJO)
    ReservationConflictChecker.java    # pure domain service (POJO)
  application/
    AvailabilityService.java           # Spring @Service, orchestration
  api/
    AvailabilityController.java        # admin CRUD endpoints
    PublicAvailabilityController.java  # public slot query endpoint
  dto/
    CreateAvailabilityRuleRequest.java
    AvailabilityRuleResponse.java
    CreateBlackoutPeriodRequest.java
    BlackoutPeriodResponse.java
    AvailableSlotsResponse.java
```

---

## Domain Entities

### AvailabilityRule
Maps to `availability_rules` table.

| Field | Type | Description |
|---|---|---|
| id | Long | PK |
| resource | Resource | FK |
| dayOfWeek | int | 1=Monday … 7=Sunday (ISO) |
| startTime | LocalTime | Opening time |
| endTime | LocalTime | Closing time |
| isOpen | boolean | false = closed all day |
| leadTimeMinutes | int | Min minutes before booking |
| sameDayCutoffTime | LocalTime | nullable; if now >= cutoff, block same-day bookings |

### BlackoutPeriod
Maps to `blackout_periods` table.

| Field | Type | Description |
|---|---|---|
| id | Long | PK |
| resource | Resource | FK |
| startAt | OffsetDateTime | Blackout start (with timezone) |
| endAt | OffsetDateTime | Blackout end (with timezone) |
| reason | String | nullable |

---

## Domain Services (Pure POJOs)

### SlotCalculator

**Input:**
- `Resource` — slotUnitMinutes, minBookingMinutes, maxBookingMinutes, bufferMinutes
- `List<AvailabilityRule>` — rules for the queried day of week
- `List<BlackoutPeriod>` — blackout periods overlapping the queried date
- `List<Reservation>` — PENDING/CONFIRMED reservations for the resource on that date
- `LocalDate date` — target date
- `int durationMinutes` — desired booking duration
- `ZoneId timezone` — from the resource's Location

**Output:** `List<AvailableSlot>` where `AvailableSlot` is a record of `(OffsetDateTime startAt, OffsetDateTime endAt)`

**Algorithm:**
1. Find the rule for the queried day. If none or `isOpen=false`, return empty list.
2. Generate candidate start times: `startTime` to `endTime - durationMinutes`, stepping by `slotUnitMinutes`.
3. For same-day queries: if `sameDayCutoffTime` is set and `now >= cutoff`, return empty list.
4. Filter out slots where `startAt - now < leadTimeMinutes`.
5. Filter out slots where `[startAt, endAt]` overlaps any blackout period.
6. Filter out slots where `[startAt - bufferMinutes, endAt + bufferMinutes]` overlaps any active reservation.
7. Return remaining slots.

### ReservationConflictChecker

**Input:**
- `List<Reservation>` — active reservations (PENDING/CONFIRMED)
- `OffsetDateTime requestedStart`
- `OffsetDateTime requestedEnd`
- `int bufferMinutes`

**Output:** `boolean` — true if conflict exists

**Logic:** Returns true if any reservation's `[startAt, endAt]` overlaps `[requestedStart - buffer, requestedEnd + buffer]`.

---

## API Endpoints

### Public (no auth)

```
GET /api/v1/public/resources/{resourceId}/available-slots
    ?date=2026-05-17
    &duration=60

Response 200:
{
  "slots": [
    { "startAt": "2026-05-17T09:00:00+09:00", "endAt": "2026-05-17T10:00:00+09:00" },
    { "startAt": "2026-05-17T10:00:00+09:00", "endAt": "2026-05-17T11:00:00+09:00" }
  ]
}
```

- `duration` must be a positive multiple of the resource's `slotUnitMinutes`. Returns 400 otherwise.
- Returns 404 if resource not found or not ACTIVE.

### Admin (JWT required, org member only)

```
POST   /api/v1/organizations/{orgId}/resources/{resourceId}/availability-rules
GET    /api/v1/organizations/{orgId}/resources/{resourceId}/availability-rules
PUT    /api/v1/organizations/{orgId}/resources/{resourceId}/availability-rules/{ruleId}
DELETE /api/v1/organizations/{orgId}/resources/{resourceId}/availability-rules/{ruleId}

POST   /api/v1/organizations/{orgId}/resources/{resourceId}/blackout-periods
GET    /api/v1/organizations/{orgId}/resources/{resourceId}/blackout-periods
DELETE /api/v1/organizations/{orgId}/resources/{resourceId}/blackout-periods/{periodId}
```

Org membership is verified by checking `OrganizationMemberRepository` in the service layer (consistent with existing Phase 2 pattern).

---

## Existing Code Changes

### ReservationService
- Replace hardcoded `"Asia/Seoul"` with `resource.getLocation().getTimezone()` when constructing `OffsetDateTime`.

### ReservationPolicyValidator
Add availability validation before conflict check:
1. Load `AvailabilityRule` for the resource + day of week. If none or `isOpen=false` → 400 `RESOURCE_UNAVAILABLE_ON_DAY`.
2. Check `startTime <= startAt.toLocalTime()` and `endAt.toLocalTime() <= endTime` → 400 `RESERVATION_OUTSIDE_BUSINESS_HOURS`.
3. Check lead time: `startAt - now >= leadTimeMinutes` → 400 `RESERVATION_LEAD_TIME_VIOLATION`.
4. If same-day and `sameDayCutoffTime` set, check `now < cutoff` → 400 `RESERVATION_PAST_SAME_DAY_CUTOFF`.
5. Load `BlackoutPeriod` overlapping `[startAt, endAt]`. If any → 400 `RESOURCE_BLACKED_OUT`.

`ReservationPolicyValidator` will receive `AvailabilityRuleRepository` and `BlackoutPeriodRepository` as constructor-injected dependencies (consistent with other validators being Spring components).

---

## Testing Strategy

### Unit Tests — SlotCalculator
- Normal case: returns slots within business hours
- `is_open=false` → empty list
- No rule for day → empty list
- Blackout covers part of day → affected slots removed
- Existing reservation + buffer blocks adjacent slot
- Lead time filters near-future slots
- Same-day cutoff exceeded → empty list
- Duration exceeds remaining business hours → no slots at end of day

### Unit Tests — ReservationConflictChecker
- No existing reservations → false
- Adjacent reservation, no buffer → false
- Adjacent reservation with buffer → true
- Fully overlapping → true

### Integration Tests (Testcontainers)
- Create availability rule → query slots → verify count
- Register blackout → slots on that date return empty
- Create reservation → that slot disappears from available list
- Attempt reservation with no availability rule → 400
- Attempt reservation outside business hours → 400
- Attempt reservation during blackout → 400
