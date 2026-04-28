# SlotOps Spring Boot Project Structure

## Technical Direction

- Framework: Spring Boot 3.x
- Language: Java 17
- Build: Gradle
- Database: PostgreSQL
- Auth: Spring Security + JWT
- Docs: springdoc-openapi
- Migration: Flyway
- Test: JUnit 5, Spring Boot Test, Testcontainers

## Packaging Principle

Use feature-first packaging with a small shared core. This keeps reservation rules, availability logic, and admin/public APIs grouped by business capability instead of by technical layer alone.

## Proposed Structure

```text
slotops-api/
  build.gradle
  settings.gradle
  src/
    main/
      java/com/jean325/slotops/
        SlotOpsApplication.java
        global/
          config/
          security/
          error/
          common/
          util/
        auth/
          api/
          application/
          domain/
          infra/
          dto/
        organization/
          api/
          application/
          domain/
          infra/
          dto/
        location/
          api/
          application/
          domain/
          infra/
          dto/
        resource/
          api/
          application/
          domain/
          infra/
          dto/
        availability/
          api/
          application/
          domain/
          infra/
          dto/
        customer/
          api/
          application/
          domain/
          infra/
          dto/
        reservation/
          api/
          application/
          domain/
          infra/
          dto/
        payment/
          api/
          application/
          domain/
          infra/
          dto/
        notification/
          application/
          domain/
          infra/
        dashboard/
          api/
          application/
          dto/
      resources/
        application.yml
        db/migration/
        messages/
    test/
      java/com/jean325/slotops/
```

## Module Responsibilities

### `global`

- Security configuration
- Exception handling
- Shared response wrapper
- Auditing and common utilities

### `auth`

- Signup, login, token refresh
- Password encoding
- JWT issue and validation

### `organization`

- Organization profile
- Member roles and membership management

### `location`

- Branch CRUD
- Public visibility and branch metadata

### `resource`

- Resource CRUD
- Resource images
- Pricing and basic constraints

### `availability`

- Weekly schedule rules
- Blackout periods
- Slot generation

### `customer`

- Customer profile
- Reservation history lookup

### `reservation`

- Booking creation
- Conflict detection
- Status transitions
- Cancellation and no-show policy checks
- Staff notes

### `payment`

- Payment records
- Deposit tracking
- Refund handling

### `notification`

- Message templates
- Email or Kakao integration adapters
- Delivery logging

### `dashboard`

- Summary metrics
- Calendar projection
- Utilization reporting

## Layer Responsibilities Per Feature

### `api`

- Controller classes
- Request validation
- Response mapping

### `application`

- Use case services
- Transaction boundaries
- Orchestration across domain objects

### `domain`

- Entities
- Domain services
- Repository interfaces
- Enums and policies

### `infra`

- JPA repositories
- External client implementations
- Persistence adapters

### `dto`

- Request and response DTOs

## Key Domain Services

- `AvailabilityCalculator`
- `ReservationConflictChecker`
- `ReservationPolicyEvaluator`
- `PricingCalculator`
- `UtilizationReportService`

## Recommended Initial Classes

```text
global/error/ApiExceptionHandler.java
global/security/JwtAuthenticationFilter.java
auth/application/AuthService.java
organization/application/OrganizationService.java
location/application/LocationService.java
resource/application/ResourceService.java
availability/application/AvailabilityService.java
reservation/application/ReservationService.java
reservation/application/ReservationStatusService.java
payment/application/PaymentService.java
dashboard/application/DashboardQueryService.java
```

## Suggested Status Enums

- `OrganizationStatus`: `ACTIVE`, `INACTIVE`
- `LocationStatus`: `ACTIVE`, `INACTIVE`
- `ResourceStatus`: `ACTIVE`, `INACTIVE`, `HIDDEN`
- `ReservationStatus`: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`
- `PaymentStatus`: `UNPAID`, `PARTIALLY_PAID`, `PAID`, `REFUNDED`
- `NotificationStatus`: `QUEUED`, `SENT`, `FAILED`

## Implementation Order

1. Global config, auth, organization bootstrap
2. Location and resource CRUD
3. Availability rules and slot calculation
4. Public reservation creation
5. Admin reservation management
6. Payment records and notifications
7. Dashboard queries and metrics

## Testing Strategy

- Unit tests for slot generation and policy rules
- Integration tests for reservation overlap and status transitions
- Repository tests with Testcontainers PostgreSQL
- API tests for public booking and admin update flows
