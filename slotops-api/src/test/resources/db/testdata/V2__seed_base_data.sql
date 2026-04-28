insert into organizations (id, name, slug, status, created_at, updated_at)
values (1, 'Demo Org', 'demo-org', 'ACTIVE', current_timestamp, current_timestamp);

insert into locations (id, organization_id, name, timezone, status, created_at, updated_at)
values (1, 1, 'Gangnam Studio', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp);

insert into resources (
    id, organization_id, location_id, resource_type, name, description, capacity,
    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
    base_price, currency, status, created_at, updated_at
) values (
    12, 1, 1, 'ROOM', 'Focus Room A', 'Quiet room', 4,
    60, 60, 180, 15,
    18000.00, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
);
