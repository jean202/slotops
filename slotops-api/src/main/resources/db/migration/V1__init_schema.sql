create table organizations (
    id bigserial primary key,
    name varchar(100) not null,
    slug varchar(50) not null unique,
    owner_user_id bigint,
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table users (
    id bigserial primary key,
    email varchar(150) not null unique,
    password_hash varchar(255) not null,
    name varchar(100) not null,
    phone varchar(30),
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table organization_members (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    user_id bigint not null references users(id),
    role varchar(20) not null,
    joined_at timestamp with time zone not null default current_timestamp
);

create table locations (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    name varchar(100) not null,
    timezone varchar(50) not null,
    phone varchar(30),
    address_line1 varchar(200),
    address_line2 varchar(200),
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table resources (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    location_id bigint not null references locations(id),
    resource_type varchar(30) not null,
    name varchar(100) not null,
    description text,
    capacity integer not null,
    slot_unit_minutes integer not null,
    min_booking_minutes integer not null,
    max_booking_minutes integer not null,
    buffer_minutes integer not null,
    base_price numeric(10,2) not null,
    currency varchar(10) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table resource_images (
    id bigserial primary key,
    resource_id bigint not null references resources(id),
    image_url varchar(500) not null,
    sort_order integer not null default 0
);

create table availability_rules (
    id bigserial primary key,
    resource_id bigint not null references resources(id),
    day_of_week integer not null,
    start_time time not null,
    end_time time not null,
    is_open boolean not null,
    lead_time_minutes integer not null,
    same_day_cutoff_time time,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table blackout_periods (
    id bigserial primary key,
    resource_id bigint not null references resources(id),
    start_at timestamp with time zone not null,
    end_at timestamp with time zone not null,
    reason varchar(200),
    created_at timestamp with time zone not null default current_timestamp
);

create table customers (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    name varchar(100) not null,
    email varchar(150),
    phone varchar(30) not null,
    memo text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table reservations (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    location_id bigint not null references locations(id),
    resource_id bigint not null references resources(id),
    customer_id bigint not null references customers(id),
    reservation_code varchar(40) not null unique,
    status varchar(20) not null,
    start_at timestamp with time zone not null,
    end_at timestamp with time zone not null,
    guest_count integer not null,
    base_amount numeric(10,2) not null,
    addon_amount numeric(10,2) not null,
    discount_amount numeric(10,2) not null,
    total_amount numeric(10,2) not null,
    special_request text,
    cancelled_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table addons (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    name varchar(100) not null,
    price numeric(10,2) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table reservation_addons (
    id bigserial primary key,
    reservation_id bigint not null references reservations(id),
    addon_id bigint not null references addons(id),
    quantity integer not null,
    unit_price numeric(10,2) not null,
    line_amount numeric(10,2) not null
);

create table payments (
    id bigserial primary key,
    reservation_id bigint not null references reservations(id),
    payment_method varchar(30),
    payment_status varchar(20) not null,
    transaction_ref varchar(100),
    paid_amount numeric(10,2) not null default 0,
    refunded_amount numeric(10,2) not null default 0,
    paid_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table notification_logs (
    id bigserial primary key,
    reservation_id bigint references reservations(id),
    customer_id bigint references customers(id),
    channel varchar(20) not null,
    template_code varchar(50) not null,
    delivery_status varchar(20) not null,
    recipient varchar(150) not null,
    sent_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp
);

create table staff_notes (
    id bigserial primary key,
    reservation_id bigint not null references reservations(id),
    author_user_id bigint not null references users(id),
    content text not null,
    created_at timestamp with time zone not null default current_timestamp
);

create index idx_locations_organization_status on locations (organization_id, status);
create index idx_resources_location_status on resources (location_id, status);
create index idx_availability_rules_resource_day on availability_rules (resource_id, day_of_week);
create index idx_blackout_periods_resource_range on blackout_periods (resource_id, start_at, end_at);
create index idx_reservations_resource_range on reservations (resource_id, start_at, end_at);
create index idx_reservations_customer_created_at on reservations (customer_id, created_at);
create index idx_reservations_status_start_at on reservations (status, start_at);
create index idx_payments_reservation_status on payments (reservation_id, payment_status);
