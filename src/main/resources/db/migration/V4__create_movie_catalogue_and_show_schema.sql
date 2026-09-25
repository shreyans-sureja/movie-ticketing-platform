CREATE TABLE movie (
    id UUID PRIMARY KEY,
    created_by_account_id UUID NOT NULL REFERENCES user_account(id),
    title VARCHAR(200) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_movie_duration CHECK (duration_minutes BETWEEN 1 AND 600),
    CONSTRAINT ck_movie_language_code CHECK (language_code ~ '^[a-z]{2,10}$')
);

CREATE UNIQUE INDEX uk_movie_title_language_duration_ci
    ON movie (LOWER(title), LOWER(language_code), duration_minutes);
CREATE INDEX ix_movie_title_ci
    ON movie (LOWER(title), id);

CREATE TABLE movie_show (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL REFERENCES movie(id),
    auditorium_id UUID NOT NULL REFERENCES auditorium(id),
    scheduled_by_account_id UUID NOT NULL REFERENCES user_account(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_movie_show_time_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_movie_show_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX ix_movie_show_movie_start_id
    ON movie_show (movie_id, starts_at, id);
CREATE INDEX ix_movie_show_auditorium_time_range
    ON movie_show (auditorium_id, starts_at, ends_at);

CREATE TABLE show_tier_price (
    show_id UUID NOT NULL REFERENCES movie_show(id),
    tier VARCHAR(20) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    PRIMARY KEY (show_id, tier),
    CONSTRAINT ck_show_tier_price_tier CHECK (tier IN ('REGULAR', 'PREMIUM')),
    CONSTRAINT ck_show_tier_price_positive CHECK (price > 0),
    CONSTRAINT ck_show_tier_price_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE show_seat (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL REFERENCES movie_show(id),
    physical_seat_id UUID NOT NULL REFERENCES physical_seat(id),
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    tier VARCHAR(20) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    availability_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_show_seat_physical UNIQUE (show_id, physical_seat_id),
    CONSTRAINT uk_show_seat_coordinate UNIQUE (show_id, row_label, seat_number),
    CONSTRAINT ck_show_seat_number CHECK (seat_number BETWEEN 1 AND 999),
    CONSTRAINT ck_show_seat_tier CHECK (tier IN ('REGULAR', 'PREMIUM')),
    CONSTRAINT ck_show_seat_price_positive CHECK (price > 0),
    CONSTRAINT ck_show_seat_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_show_seat_availability CHECK (availability_status IN ('AVAILABLE'))
);

CREATE INDEX ix_show_seat_show_order
    ON show_seat (show_id, row_label, seat_number, id);
CREATE INDEX ix_show_seat_show_availability
    ON show_seat (show_id, availability_status);
