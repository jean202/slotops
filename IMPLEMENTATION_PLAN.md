# SlotOps Implementation Plan

## Overview
Build a reservation operations SaaS for small offline businesses. Focus on time-slot based resource management with multi-tenant support.

**Timeline**: Phase 1-4 implementation (MVP → Full feature set)

---

## Phase 1: Foundation & Authentication (Week 1)

### Goals
- Set up global security and error handling
- Implement user authentication (signup/login/token refresh)
- Establish JWT-based authorization
- Create organization bootstrap flow

### Tasks
1. **Global Configuration** `global/`
   - [ ] `GlobalExceptionHandler` - centralized error handling, consistent API error responses
   - [ ] `JwtAuthenticationFilter` - intercept and validate JWT tokens
   - [ ] `JwtTokenProvider` - issue, validate, refresh JWT tokens
   - [ ] `SecurityConfig` - Spring Security configuration with JWT
   - [ ] `ApiResponse<T>` wrapper for all API responses
   - [ ] Common constants and error codes

2. **Auth Module** `auth/`
   - [ ] `User` entity (email, password_hash, name, phone, status)
   - [ ] `UserRepository` interface + JPA implementation
   - [ ] `AuthService` - signup, login, token refresh logic
   - [ ] `AuthController` - POST /auth/signup, /auth/login, /auth/refresh
   - [ ] Request/Response DTOs (SignupRequest, LoginResponse, etc.)
   - [ ] Password encoding with BCrypt

3. **Organization Module Bootstrap** `organization/`
   - [ ] `Organization` entity with status tracking
   - [ ] `OrganizationMember` entity (user + role mapping)
   - [ ] `OrganizationStatus` enum (ACTIVE, INACTIVE)
   - [ ] `OrganizationMemberRole` enum (OWNER, MANAGER, STAFF)
   - [ ] `OrganizationRepository`, `OrganizationMemberRepository` JPA
   - [ ] `OrganizationService` - create organization, add members
   - [ ] `OrganizationController` - POST /organizations, GET /organizations/{id}

### Testing
- Unit tests for JWT token generation and validation
- Integration test for signup → login → authenticated request flow
- Test organization creation with owner assignment

### Acceptance Criteria
- User can sign up with email/password
- JWT token issued and can authenticate subsequent requests
- Organization created with owner assigned
- Invalid tokens rejected with 401

---

## Phase 2: Core Data Setup (Week 1-2)

### Goals
- Implement CRUD for locations and resources
- Set up basic validation and status management
- Enable multi-location, multi-resource data model

### Tasks
1. **Location Module** `location/`
   - [ ] `Location` entity with timezone, address fields
   - [ ] `LocationStatus` enum (ACTIVE, INACTIVE)
   - [ ] `LocationRepository` JPA
   - [ ] `LocationService` - CRUD operations with org context
   - [ ] `LocationController` - POST, GET, PUT, DELETE /locations
   - [ ] Validation: name required, timezone valid, org_id authorized
   - [ ] Request/Response DTOs

2. **Resource Module** `resource/`
   - [ ] `Resource` entity (type, capacity, pricing, slot constraints)
   - [ ] `ResourceType` enum (ROOM, COURT, STUDIO, EQUIPMENT)
   - [ ] `ResourceStatus` enum (ACTIVE, INACTIVE, HIDDEN)
   - [ ] `ResourceImage` entity for photo URLs
   - [ ] `ResourceRepository`, `ResourceImageRepository` JPA
   - [ ] `ResourceService` - CRUD with location/org context
   - [ ] `ResourceController` - POST, GET, PUT /resources
   - [ ] Request/Response DTOs including pricing

3. **Permission Model**
   - [ ] `@RequireOrgAccess` annotation to check org membership
   - [ ] `@RequireRole` annotation to check staff role
   - [ ] Service-layer authorization checks in all CRUD

### Testing
- Integration tests for location CRUD with org isolation
- Integration tests for resource CRUD with location isolation
- Test permission denial when accessing other org's data
- Test cascading updates (location active → resource visibility)

### Acceptance Criteria
- Operators can create locations and resources
- Data isolated by organization
- Resource types and status tracked correctly
- Pricing and slot constraints stored

---

## Phase 3: Availability & Booking Logic (Week 2-3)

### Goals
- Implement weekly availability rules and blackout periods
- Build slot generation and conflict detection
- Enable customer to see available time slots

### Tasks
1. **Availability Module** `availability/`
   - [ ] `AvailabilityRule` entity (day_of_week, start_time, end_time, lead_time, same_day_cutoff)
   - [ ] `BlackoutPeriod` entity (date ranges when resource unavailable)
   - [ ] `AvailabilityRuleRepository`, `BlackoutPeriodRepository` JPA
   - [ ] `AvailabilityService` - CRUD for rules and blackouts
   - [ ] `AvailabilityController` - manage rules and blackouts

2. **Slot Generation & Conflict Logic** (Core Domain)
   - [ ] `SlotCalculator` domain service
     - Generate available slots for a resource on a given date
     - Consider: availability rules, blackout periods, buffer times, min/max duration
     - Account for timezone
   - [ ] `ReservationConflictChecker` domain service
     - Detect overlaps with existing reservations
     - Account for cleanup buffer
   - [ ] `AvailableSlotsRequest` DTO (resource_id, date, duration_minutes)
   - [ ] `AvailableSlotResponse` DTO (list of [start_time, end_time] slots)
   - [ ] Public API: GET /public/resources/{id}/available-slots?date=YYYY-MM-DD&duration=60

3. **Reservation Module - Part 1** `reservation/`
   - [ ] `Reservation` entity (basic fields from schema)
   - [ ] `ReservationStatus` enum (PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW)
   - [ ] `ReservationRepository` JPA
   - [ ] `ReservationCodeGenerator` - generate unique booking codes (e.g., RES-20250503-ABC123)
   - [ ] DTOs: `PublicCreateReservationRequest`, `PublicReservationResponse`

### Testing
- Unit tests for slot generation with various availability rules
- Unit test conflict detection with overlapping reservations and buffers
- Integration test: create availability rules → query slots → verify results
- Test blackout period blocks all slots on that date
- Test same-day cutoff prevents booking after cutoff time

### Acceptance Criteria
- Available slots correctly calculated respecting all constraints
- Conflict detection works with cleanup buffers
- Customers can query available slots for a resource
- Availability rules and blackouts manageable via admin API

---

## Phase 4: Reservation Management & Transactions (Week 3-4)

### Goals
- Implement customer reservation creation and admin approval flow
- Add payment status tracking and policy validation
- Enable cancellation, no-show, and status transitions

### Tasks
1. **Reservation Module - Part 2** `reservation/`
   - [ ] `ReservationService` - create, confirm, cancel, complete, mark_no_show
   - [ ] `ReservationPolicyValidator` domain service
     - Validate lead time constraints
     - Validate min/max booking duration
     - Validate customer can book (e.g., no overlapping confirmed bookings)
     - Check refund policies on cancellation
   - [ ] `PublicReservationController` - POST /public/reservations (customer booking)
   - [ ] `AdminReservationController` - POST /admin/reservations/{id}/confirm, /cancel, /mark-completed
   - [ ] Implement idempotency with reservation_code as unique key

2. **Customer Module** `customer/`
   - [ ] `Customer` entity (name, email, phone, memo)
   - [ ] `CustomerRepository` JPA
   - [ ] `CustomerService` - create or lookup customer
   - [ ] Link customer to organization for history tracking

3. **Payment Module** `payment/`
   - [ ] `Payment` entity (payment_status, paid_amount, refunded_amount, transaction_ref)
   - [ ] `PaymentStatus` enum (UNPAID, PARTIALLY_PAID, PAID, REFUNDED)
   - [ ] `PaymentRepository` JPA
   - [ ] `PaymentService` - create payment record on reservation creation
   - [ ] Mark UNPAID initially, allow admin to mark PAID (offline payment for MVP)
   - [ ] `PaymentController` - GET /admin/reservations/{id}/payment, PUT to update status

4. **Addon Support** `reservation/`
   - [ ] `Addon` entity (name, price, status)
   - [ ] `ReservationAddon` junction (reservation + addon + quantity + line_amount)
   - [ ] `AddonService`, `AddonRepository` JPA
   - [ ] `PublicCreateReservationRequest` includes optional addon_ids array
   - [ ] Calculate total_amount = base_amount + addon_amount - discount_amount

5. **Notification Skeleton** `notification/`
   - [ ] `NotificationLog` entity (channel, template_code, delivery_status, recipient)
   - [ ] `NotificationService` - log notification intent (no actual email send yet)
   - [ ] Trigger logs on: reservation created, confirmed, cancelled
   - [ ] Placeholder for integration with email provider

6. **Reservation Workflow Endpoints**
   - [ ] POST /public/reservations - create (PENDING status)
   - [ ] GET /admin/reservations - list with filters (status, date range, customer)
   - [ ] POST /admin/reservations/{id}/confirm - approve (PENDING → CONFIRMED)
   - [ ] POST /admin/reservations/{id}/cancel - reject/cancel with refund logic
   - [ ] POST /admin/reservations/{id}/complete - mark completed after use (CONFIRMED → COMPLETED)
   - [ ] POST /admin/reservations/{id}/no-show - mark no-show (for policy tracking)
   - [ ] GET /customers/{id}/reservations - customer history

### Testing
- Integration test: create reservation → verify PENDING → confirm → CONFIRMED
- Integration test: cancellation before cutoff → full refund, after → partial
- Integration test: addon pricing included in total_amount
- Test idempotency: duplicate create requests return same reservation
- Test policy: prevent double-booking same time slot
- Test policy: enforce lead time constraints

### Acceptance Criteria
- Customers can book available slots with complete form
- Admin can approve/reject reservations
- Payment status tracked (UNPAID → PAID)
- Cancellation respects policy rules (refund amount based on timing)
- Notification logs created for key events
- No double-bookings possible

---

## Phase 5: Admin Dashboard & Reporting (Week 4)

### Goals
- Provide operators visibility into utilization and revenue
- Enable calendar view and metrics

### Tasks
1. **Dashboard Module** `dashboard/`
   - [ ] `DashboardQueryService` - aggregation queries (no persistence)
   - [ ] `UtilizationMetrics` DTO (total slots, booked, utilization %)
   - [ ] `RevenueMetrics` DTO (total_revenue, payment_status breakdown)
   - [ ] `DashboardController`:
     - GET /admin/dashboard/summary - daily metrics
     - GET /admin/dashboard/utilization?location_id=&date_range= - resource utilization
     - GET /admin/dashboard/revenue - payment status breakdown
     - GET /admin/dashboard/calendar?location_id=&month= - calendar view data

2. **Staff Notes** `reservation/`
   - [ ] `StaffNote` entity (content, author, created_at)
   - [ ] `StaffNoteService` - add notes to reservation
   - [ ] POST /admin/reservations/{id}/notes - add note

### Testing
- Integration test: verify metrics calculated correctly with sample reservations
- Test date range filtering works across timezones

### Acceptance Criteria
- Operator sees dashboard with key metrics
- Calendar shows booked/available slots
- Revenue reporting by status accurate

---

## Implementation Priorities & Notes

### Must-Have (MVP)
1. Auth + Organization
2. Location + Resource CRUD
3. Availability rules + slot calculation + conflict detection
4. Customer reservation creation (public API)
5. Admin approval/cancellation
6. Payment status tracking
7. Notification logging (no actual send yet)

### Nice-to-Have
1. Dashboard/reporting
2. Staff notes
3. Addons
4. Coupon/discount support

### Technical Decisions
- Use Spring Data JPA for data access
- Domain services for complex business logic (slot calculation, conflict detection, policy validation)
- Transactional boundaries at service layer
- Repository interfaces in `domain/`, implementations in `infra/`
- DTOs for all API requests/responses
- Unit tests for domain services, integration tests for full flows
- Testcontainers for PostgreSQL in integration tests

### Key Files to Create First
1. `global/config/SecurityConfig.java`
2. `global/error/ApiExceptionHandler.java`
3. `global/error/ErrorResponse.java`
4. `global/security/JwtTokenProvider.java`
5. `global/security/JwtAuthenticationFilter.java`
6. `auth/application/AuthService.java`
7. `availability/domain/SlotCalculator.java`
8. `availability/domain/ReservationConflictChecker.java`
9. `reservation/application/ReservationPolicyValidator.java`

---

## Testing Strategy

- **Unit Tests**: Domain services (slot calculation, conflict detection, policy validation)
- **Integration Tests**: Full workflows (signup → create org → add location → create resource → available slots → reservation → approval)
- **Testcontainers**: PostgreSQL for integration tests
- **API Tests**: Public and admin endpoints with valid/invalid inputs

---

## Success Criteria by Phase

**Phase 1**: User can sign up, create organization, log in with JWT
**Phase 2**: Operator can create locations and resources with full CRUD
**Phase 3**: Customer can query available slots with all constraints respected
**Phase 4**: End-to-end reservation flow works (create → approve → complete)
**Phase 5**: Operator can view utilization metrics and calendar

