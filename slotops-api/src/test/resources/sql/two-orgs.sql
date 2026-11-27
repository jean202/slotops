-- Two fully separate tenants for cross-organization access tests.
-- Org 2001 "alpha" (user 2001 is STAFF) and org 2002 "beta" (user 2002 is OWNER).
-- Every child row uses the same id as its org so the owner is obvious in tests.
-- Ids are far above anything the identity sequences will generate.
insert into users (id, email, password_hash, name, status, created_at, updated_at) values
    (2001, 'alpha@example.com', 'x', 'Alpha Staff', 'ACTIVE', current_timestamp, current_timestamp),
    (2002, 'beta@example.com', 'x', 'Beta Owner', 'ACTIVE', current_timestamp, current_timestamp);

insert into organizations (id, name, slug, status, created_at, updated_at) values
    (2001, 'Alpha', 'alpha', 'ACTIVE', current_timestamp, current_timestamp),
    (2002, 'Beta', 'beta', 'ACTIVE', current_timestamp, current_timestamp);

insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at) values
    (2001, 2001, 'STAFF', current_timestamp, current_timestamp, current_timestamp),
    (2002, 2002, 'OWNER', current_timestamp, current_timestamp, current_timestamp);

insert into locations (id, organization_id, name, timezone, status, created_at, updated_at) values
    (2001, 2001, 'Alpha Studio', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp),
    (2002, 2002, 'Beta Studio', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp);

insert into resources (
    id, organization_id, location_id, resource_type, name, capacity,
    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
    base_price, currency, status, created_at, updated_at
) values
    (2001, 2001, 2001, 'ROOM', 'Alpha Room', 4, 60, 60, 180, 0, 10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp),
    (2002, 2002, 2002, 'ROOM', 'Beta Room', 4, 60, 60, 180, 0, 10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp);

insert into availability_rules (
    id, resource_id, day_of_week, start_time, end_time, is_open, lead_time_minutes, created_at, updated_at
) values
    (2001, 2001, 1, '09:00', '18:00', true, 0, current_timestamp, current_timestamp),
    (2002, 2002, 1, '09:00', '18:00', true, 0, current_timestamp, current_timestamp);

insert into blackout_periods (id, resource_id, start_at, end_at, reason, created_at) values
    (2001, 2001, TIMESTAMP WITH TIME ZONE '2026-07-01 00:00:00+09:00', TIMESTAMP WITH TIME ZONE '2026-07-02 00:00:00+09:00', 'Alpha holiday', current_timestamp),
    (2002, 2002, TIMESTAMP WITH TIME ZONE '2026-07-01 00:00:00+09:00', TIMESTAMP WITH TIME ZONE '2026-07-02 00:00:00+09:00', 'Beta holiday', current_timestamp);

insert into customers (id, organization_id, name, email, phone, created_at, updated_at) values
    (2001, 2001, 'Alpha Customer', 'a@example.com', '010-0000-2001', current_timestamp, current_timestamp),
    (2002, 2002, 'Beta Customer', 'b@example.com', '010-0000-2002', current_timestamp, current_timestamp);

insert into reservations (
    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
    created_at, updated_at
) values
    (2001, 2001, 2001, 2001, 2001, 'RSV-ALPHA', 'PENDING',
     TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00', TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
     2, 20000.00, 0, 0, 20000.00, current_timestamp, current_timestamp),
    (2002, 2002, 2002, 2002, 2002, 'RSV-BETA', 'PENDING',
     TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00', TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
     2, 20000.00, 0, 0, 20000.00, current_timestamp, current_timestamp);

insert into payments (id, reservation_id, payment_status, paid_amount, refunded_amount, created_at, updated_at) values
    (2001, 2001, 'UNPAID', 0, 0, current_timestamp, current_timestamp),
    (2002, 2002, 'UNPAID', 0, 0, current_timestamp, current_timestamp);

insert into addons (id, organization_id, name, price, status, created_at, updated_at) values
    (2001, 2001, 'Alpha Projector', 5000, 'ACTIVE', current_timestamp, current_timestamp),
    (2002, 2002, 'Beta Projector', 5000, 'ACTIVE', current_timestamp, current_timestamp);

insert into staff_notes (id, reservation_id, author_user_id, content, created_at) values
    (2002, 2002, 2002, 'Beta internal note', current_timestamp);
