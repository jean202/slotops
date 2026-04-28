# SlotOps REST API Draft

## API Principles

- Base path: `/api/v1`
- JSON request and response
- Bearer token authentication for operator endpoints
- Public reservation search and creation endpoints are separated from admin endpoints

## Auth

### `POST /api/v1/auth/signup`

Create an operator account and initial organization.

### `POST /api/v1/auth/login`

Authenticate with email and password.

### `POST /api/v1/auth/refresh`

Refresh access token.

### `GET /api/v1/me`

Return current user profile and organization memberships.

## Organizations

### `GET /api/v1/organizations/{organizationId}`

Get organization profile.

### `PATCH /api/v1/organizations/{organizationId}`

Update organization profile.

## Locations

### `GET /api/v1/admin/locations`

List locations for current organization.

### `POST /api/v1/admin/locations`

Create a location.

Example request:

```json
{
  "name": "Gangnam Studio",
  "timezone": "Asia/Seoul",
  "phone": "02-1234-5678",
  "addressLine1": "123 Teheran-ro",
  "addressLine2": "5F"
}
```

### `GET /api/v1/admin/locations/{locationId}`

Get location detail.

### `PATCH /api/v1/admin/locations/{locationId}`

Update location.

### `DELETE /api/v1/admin/locations/{locationId}`

Soft-delete location.

## Resources

### `GET /api/v1/admin/resources`

List resources with filters:

- `locationId`
- `resourceType`
- `status`

### `POST /api/v1/admin/resources`

Create a resource.

```json
{
  "locationId": 1,
  "resourceType": "ROOM",
  "name": "Focus Room A",
  "description": "Quiet room for up to 4 people",
  "capacity": 4,
  "slotUnitMinutes": 60,
  "minBookingMinutes": 60,
  "maxBookingMinutes": 180,
  "bufferMinutes": 15,
  "basePrice": 18000,
  "currency": "KRW"
}
```

### `GET /api/v1/admin/resources/{resourceId}`

Get resource detail.

### `PATCH /api/v1/admin/resources/{resourceId}`

Update resource.

### `DELETE /api/v1/admin/resources/{resourceId}`

Soft-delete resource.

## Availability and Blackouts

### `GET /api/v1/admin/resources/{resourceId}/availability-rules`

List recurring availability rules.

### `PUT /api/v1/admin/resources/{resourceId}/availability-rules`

Replace weekly availability rules for a resource.

```json
{
  "rules": [
    {
      "dayOfWeek": 1,
      "startTime": "09:00",
      "endTime": "22:00",
      "isOpen": true,
      "leadTimeMinutes": 120,
      "sameDayCutoffTime": "20:00"
    }
  ]
}
```

### `GET /api/v1/admin/resources/{resourceId}/blackouts`

List blackout periods.

### `POST /api/v1/admin/resources/{resourceId}/blackouts`

Create blackout period.

### `DELETE /api/v1/admin/resources/{resourceId}/blackouts/{blackoutId}`

Delete blackout period.

## Public Discovery

### `GET /api/v1/public/locations`

List public locations.

### `GET /api/v1/public/resources`

Search public resources.

Filters:

- `locationId`
- `date`
- `partySize`
- `resourceType`

### `GET /api/v1/public/resources/{resourceId}/availability`

Return available slots for a date.

Example response:

```json
{
  "resourceId": 12,
  "date": "2026-05-10",
  "slots": [
    {
      "startAt": "2026-05-10T10:00:00+09:00",
      "endAt": "2026-05-10T11:00:00+09:00",
      "price": 18000,
      "available": true
    }
  ]
}
```

## Customers

### `GET /api/v1/admin/customers`

List customers with search by name, phone, or email.

### `GET /api/v1/admin/customers/{customerId}`

Get customer profile and reservation history.

## Reservations

### `POST /api/v1/public/reservations`

Create reservation from public booking page.

```json
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
  "addonSelections": [
    {
      "addonId": 3,
      "quantity": 1
    }
  ],
  "specialRequest": "Need HDMI cable"
}
```

### `GET /api/v1/public/reservations/{reservationCode}`

Get public reservation detail for customer lookup.

### `POST /api/v1/public/reservations/{reservationCode}/cancel`

Customer cancellation endpoint.

### `GET /api/v1/admin/reservations`

List reservations with filters:

- `locationId`
- `resourceId`
- `status`
- `from`
- `to`

### `GET /api/v1/admin/reservations/{reservationId}`

Get reservation detail.

### `PATCH /api/v1/admin/reservations/{reservationId}/status`

Update reservation status.

```json
{
  "status": "CONFIRMED",
  "reason": "Payment verified"
}
```

### `PATCH /api/v1/admin/reservations/{reservationId}`

Update reservation notes, guest count, or schedule if policy allows.

### `POST /api/v1/admin/reservations/{reservationId}/notes`

Add staff note.

## Payments

### `POST /api/v1/admin/reservations/{reservationId}/payments`

Create manual payment record.

### `GET /api/v1/admin/reservations/{reservationId}/payments`

List payment history.

### `POST /api/v1/admin/payments/{paymentId}/refund`

Register refund.

## Dashboard

### `GET /api/v1/admin/dashboard/summary`

Return counts and totals for today and this week.

### `GET /api/v1/admin/dashboard/calendar`

Return reservation data grouped by day and resource.

### `GET /api/v1/admin/dashboard/utilization`

Return utilization metrics by location and resource.

## Example Reservation Response

```json
{
  "id": 345,
  "reservationCode": "RSV-20260510-9X2A",
  "status": "PENDING",
  "location": {
    "id": 1,
    "name": "Gangnam Studio"
  },
  "resource": {
    "id": 12,
    "name": "Focus Room A"
  },
  "customer": {
    "name": "Kim Minji",
    "phone": "010-1234-5678"
  },
  "startAt": "2026-05-10T10:00:00+09:00",
  "endAt": "2026-05-10T12:00:00+09:00",
  "guestCount": 3,
  "pricing": {
    "baseAmount": 36000,
    "addonAmount": 5000,
    "discountAmount": 0,
    "totalAmount": 41000
  },
  "paymentStatus": "UNPAID"
}
```

## Error Model

All errors use a shared response shape.

```json
{
  "code": "RESERVATION_SLOT_UNAVAILABLE",
  "message": "The selected time slot is no longer available.",
  "fieldErrors": []
}
```

## Priority Business Rules

- Reservation cannot overlap existing confirmed or pending bookings for the same resource.
- Resource buffer time must be enforced before and after reservation.
- Same-day booking cutoff must be checked against location timezone.
- Customer cancellation is blocked after policy cutoff.
- Soft-deleted resources are hidden from public discovery.
