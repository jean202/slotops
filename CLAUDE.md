# SlotOps - Implementation Guide

## Project Overview

**SlotOps** is a reservation operations SaaS for small offline businesses (studios, practice rooms, sports courts, meeting rooms). The system manages time-slot based resource bookings, operator workflows, and payment tracking.

**Stack**: Spring Boot 3.x, PostgreSQL, JWT auth, Gradle, Testcontainers

**Status**: Initial scaffold complete. Ready for Phase 1 (Auth + Organization) implementation.

---

## Current State

### Completed
- ✅ Product spec and architecture design (`docs/`)
- ✅ Database schema with Flyway migration (`src/main/resources/db/migration/V1__init_schema.sql`)
- ✅ Partial domain entities (Organization, Location, Resource, Reservation, etc.)
- ✅ Gradle project structure with Spring Boot 3.x

### In Progress
- Phase 1: Auth + Organization (JWT, signup/login, org creation)

### Next Steps
- See `IMPLEMENTATION_PLAN.md` for detailed Phase-by-Phase breakdown

---

## Key Architecture Decisions

1. **Feature-First Packaging**: Organize code by business domain (`auth/`, `organization/`, `location/`, etc.), not by technical layer.
2. **Domain Services for Business Logic**: 
   - `SlotCalculator` - generate available time slots
   - `ReservationConflictChecker` - detect booking overlaps
   - `ReservationPolicyValidator` - enforce business rules
3. **Repository Pattern**: Interfaces in `domain/`, implementations in `infra/`
4. **DTOs for All APIs**: Request/Response DTOs in each feature's `dto/` package
5. **Transactional Boundaries**: Service layer handles transactions
6. **Multi-Tenant Isolation**: All queries scoped by organization_id

---

## Module Responsibilities

| Module | Purpose |
|--------|---------|
| `global/` | Security, exception handling, shared utilities |
| `auth/` | User signup, login, JWT token management |
| `organization/` | Tenant account, staff roles, membership |
| `location/` | Branch/venue CRUD, timezone, metadata |
| `resource/` | Reservable unit (room, court, studio, equipment) |
| `availability/` | Weekly rules, blackout periods, slot calculation |
| `customer/` | Customer profiles, booking history |
| `reservation/` | Booking creation, approval, cancellation, status tracking |
| `payment/` | Payment records, status (UNPAID/PAID/REFUNDED) |
| `notification/` | Event logging, notification templates (email integration ready) |
| `dashboard/` | Metrics, utilization, revenue reporting |

---

## Development Workflow

1. **Check IMPLEMENTATION_PLAN.md** for current phase goals and tasks
2. **Work Phase-by-Phase** (1-5), completing all tasks before advancing
3. **Write Tests First** (or alongside) - unit tests for domain logic, integration tests for workflows
4. **Commit After Each Feature** with conventional commits (feat:, fix:, test:, etc.)
5. **Verify Against Acceptance Criteria** before marking phase complete

---

## Testing Strategy

- **Unit Tests**: Domain services (slot generation, conflict detection, policy rules)
- **Integration Tests**: Full workflows (signup → org → location → resource → reservation → approval)
- **Testcontainers**: PostgreSQL for data access layer tests
- **API Tests**: Happy path and error cases for public/admin endpoints

---

## Common Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run integration tests (with Testcontainers)
./gradlew integrationTest

# Run app locally
./gradlew bootRun

# Check linting/formatting
./gradlew spotlessCheck
```

---

## Key Files to Know

- **Schema**: `src/main/resources/db/migration/V1__init_schema.sql`
- **Build Config**: `build.gradle.kts`
- **App Config**: `src/main/resources/application.yml`
- **Entities**: `src/main/java/com/jean325/slotops/{module}/domain/`
- **Controllers**: `src/main/java/com/jean325/slotops/{module}/api/`
- **Business Logic**: `src/main/java/com/jean325/slotops/{module}/application/`

---

## Notes for Future Sessions

- Prioritize slot calculation and conflict detection logic (most complex business domain)
- Keep permission/authorization checks close to data access (prevent org data leaks)
- Use `@RequireOrgAccess` and `@RequireRole` annotations consistently
- Payment module is initially simple (offline status tracking); ready for Stripe/PG integration later
- Notification module logs events; email/SMS provider integration deferred

