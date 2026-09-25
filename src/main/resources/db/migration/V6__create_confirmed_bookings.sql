CREATE TABLE booking (
    id UUID PRIMARY KEY,
    source_hold_id UUID NOT NULL REFERENCES seat_hold(id),
    show_id UUID NOT NULL REFERENCES movie_show(id),
    customer_account_id UUID NOT NULL REFERENCES user_account(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_booking_status CHECK (status IN ('CONFIRMED')),
    CONSTRAINT ck_booking_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_booking_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uk_booking_source_hold
    ON booking (source_hold_id);
CREATE INDEX ix_booking_customer_confirmed_id
    ON booking (customer_account_id, confirmed_at DESC, id DESC);

ALTER TABLE show_seat
    ADD COLUMN current_booking_id UUID REFERENCES booking(id);

ALTER TABLE show_seat
    DROP CONSTRAINT ck_show_seat_availability,
    DROP CONSTRAINT ck_show_seat_hold_consistency;

ALTER TABLE show_seat
    ADD CONSTRAINT ck_show_seat_availability
        CHECK (availability_status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    ADD CONSTRAINT ck_show_seat_ownership_consistency
        CHECK (
            (availability_status = 'AVAILABLE'
                AND current_hold_id IS NULL
                AND current_booking_id IS NULL)
            OR
            (availability_status = 'HELD'
                AND current_hold_id IS NOT NULL
                AND current_booking_id IS NULL)
            OR
            (availability_status = 'BOOKED'
                AND current_hold_id IS NULL
                AND current_booking_id IS NOT NULL)
        );

CREATE TABLE booking_item (
    booking_id UUID NOT NULL REFERENCES booking(id),
    show_seat_id UUID NOT NULL REFERENCES show_seat(id),
    unit_price NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (booking_id, show_seat_id),
    CONSTRAINT ck_booking_item_unit_price CHECK (unit_price >= 0)
);

ALTER TABLE show_seat
    ADD CONSTRAINT fk_show_seat_current_booking_item
        FOREIGN KEY (current_booking_id, id)
        REFERENCES booking_item (booking_id, show_seat_id);
