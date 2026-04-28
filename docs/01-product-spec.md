# SlotOps Product Spec

## 1. Service Name

- Product name: `SlotOps`
- Tagline: `Run bookings for spaces and resources without spreadsheet chaos.`

## 2. Product Summary

SlotOps is a SaaS for operators who manage physical spaces or rentable resources. It supports resource setup, operating hours, pricing, blackout dates, booking approval, payment status, and automated notifications.

The product is designed around operational efficiency for businesses that rent by time slot, not around event admission or ticket issuance.

## 3. Target Users

### Operator-side users

- Business owner
- Branch manager
- Staff member

### Customer-side users

- Guest user making a reservation
- Registered customer with reservation history

## 4. Problems to Solve

- Operators manually manage schedules in spreadsheets or chat.
- Double booking occurs when availability rules are inconsistent.
- Different resources need different slot lengths, cleanup buffers, and lead times.
- Staff spend too much time on confirmation, cancellation, and reminder messages.
- Customers do not know which resource is available across multiple locations.

## 5. Scope

### In Scope

- Multi-location resource management
- Resource-level availability and blackout rules
- Time-slot based reservations
- Approval and cancellation workflow
- Payment status tracking
- Reminder and confirmation notifications
- Admin dashboard and calendar view

### Out of Scope

- Event ticketing
- Seat map selection
- Queue room for high-traffic drops
- Kiosk or POS flows
- Corporate CRM and settlement complexity

## 6. Core Concepts

- `Organization`: tenant account that owns locations and staff
- `Location`: physical branch or venue
- `Resource`: reservable unit such as room, court, studio, or equipment
- `Availability Rule`: recurring business hours and slot constraints
- `Blackout`: date or period where reservation is not allowed
- `Reservation`: booking record for a customer and resource
- `Addon`: optional paid extra for a reservation
- `Payment`: reservation payment or deposit
- `Notification`: message delivery log

## 7. Core Features

### 7.1 Organization and Staff

- Create organization profile
- Invite staff members
- Assign roles: `OWNER`, `MANAGER`, `STAFF`

### 7.2 Location Management

- Register multiple locations
- Set address, contact, timezone, and business notes
- Mark each location active or inactive

### 7.3 Resource Management

- Create resources by type
  - Room
  - Court
  - Studio
  - Equipment
- Set capacity, base price, slot unit, cleanup buffer, and booking policy
- Attach photos and description

### 7.4 Availability Rules

- Set weekly business hours by resource
- Set minimum and maximum reservation duration
- Set lead time and same-day cutoff
- Apply cleanup buffer between reservations
- Override schedule with blackout dates

### 7.5 Reservation Flow

- Customer selects location, resource, date, start time, and duration
- System validates availability
- Customer enters contact information and optional requests
- Reservation is created in one of these statuses
  - `PENDING`
  - `CONFIRMED`
  - `CANCELLED`
  - `COMPLETED`
  - `NO_SHOW`

### 7.6 Payment and Deposit

- Track `UNPAID`, `PARTIALLY_PAID`, `PAID`, `REFUNDED`
- Support deposit amount and due date
- Operators can confirm offline payment if needed

### 7.7 Notifications

- Reservation requested
- Reservation confirmed
- Reservation cancelled
- Reminder before reservation start
- Follow-up after use

### 7.8 Admin Dashboard

- Daily and weekly reservation calendar
- Upcoming reservations list
- Resource utilization view
- Reservation status breakdown

## 8. Differentiators

- Buffer-time aware scheduling
- Resource bundle reservation support
  - Example: studio + lighting kit + tripod
- Policy automation for cancellation and no-show handling

## 9. MVP Definition

### MVP Must-Haves

- Organization, location, and resource CRUD
- Availability and blackout management
- Customer reservation creation
- Admin reservation approval and cancellation
- Basic payment status tracking
- Email notification hooks
- Calendar-style admin view

### MVP Nice-to-Haves

- Coupon support
- Recurring reservation templates
- Resource bundle pricing

## 10. Success Metrics

- Reservation completion rate
- Double-booking incident count
- Staff time spent per reservation
- Resource utilization rate
- Cancellation and no-show ratio

## 11. Portfolio Framing

Use this project as:

- A general reservation operations system for offline businesses
- A case study in multi-tenant SaaS design
- A backend-heavy product focused on scheduling rules, policy enforcement, and operational tooling

Avoid describing it as:

- A refactor of a company ticketing platform
- A derivative of event booking, seat booking, or admission processing
