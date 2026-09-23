CREATE TABLE city (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    state_or_ut VARCHAR(120) NOT NULL
);

CREATE UNIQUE INDEX uk_city_name_state_ci
    ON city (LOWER(name), LOWER(state_or_ut));

CREATE TABLE theatre (
    id UUID PRIMARY KEY,
    owner_account_id UUID NOT NULL REFERENCES user_account(id),
    city_id BIGINT NOT NULL REFERENCES city(id),
    name VARCHAR(150) NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),
    postal_code VARCHAR(6) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_theatre_postal_code CHECK (postal_code ~ '^[0-9]{6}$')
);

CREATE INDEX ix_theatre_owner_name_id
    ON theatre (owner_account_id, name, id);
CREATE INDEX ix_theatre_city
    ON theatre (city_id);
CREATE UNIQUE INDEX uk_theatre_owner_city_name_ci
    ON theatre (owner_account_id, city_id, LOWER(name));

CREATE TABLE auditorium (
    id UUID PRIMARY KEY,
    theatre_id UUID NOT NULL REFERENCES theatre(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_auditorium_theatre_name_id
    ON auditorium (theatre_id, name, id);
CREATE UNIQUE INDEX uk_auditorium_theatre_name_ci
    ON auditorium (theatre_id, LOWER(name));

CREATE TABLE physical_seat (
    id UUID PRIMARY KEY,
    auditorium_id UUID NOT NULL REFERENCES auditorium(id),
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    tier VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_physical_seat_row_label CHECK (row_label ~ '^[A-Z]{1,10}$'),
    CONSTRAINT ck_physical_seat_number CHECK (seat_number BETWEEN 1 AND 999),
    CONSTRAINT ck_physical_seat_tier CHECK (tier IN ('REGULAR', 'PREMIUM')),
    CONSTRAINT uk_physical_seat_coordinate UNIQUE (auditorium_id, row_label, seat_number)
);

CREATE INDEX ix_physical_seat_auditorium_row_number_id
    ON physical_seat (auditorium_id, row_label, seat_number, id);
