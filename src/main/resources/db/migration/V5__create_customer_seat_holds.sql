CREATE TABLE seat_hold (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL REFERENCES movie_show(id),
    customer_account_id UUID NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_seat_hold_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_seat_hold_customer_created_id
    ON seat_hold (customer_account_id, created_at DESC, id);
CREATE INDEX ix_seat_hold_show_expiry_id
    ON seat_hold (show_id, expires_at, id);

ALTER TABLE show_seat
    DROP CONSTRAINT ck_show_seat_availability;

ALTER TABLE show_seat
    ADD COLUMN current_hold_id UUID REFERENCES seat_hold(id);

ALTER TABLE show_seat
    ADD CONSTRAINT ck_show_seat_availability
        CHECK (availability_status IN ('AVAILABLE', 'HELD')),
    ADD CONSTRAINT ck_show_seat_hold_consistency
        CHECK (
            (availability_status = 'AVAILABLE' AND current_hold_id IS NULL)
            OR
            (availability_status = 'HELD' AND current_hold_id IS NOT NULL)
        );

CREATE INDEX ix_show_seat_current_hold
    ON show_seat (current_hold_id);

CREATE TABLE seat_hold_item (
    hold_id UUID NOT NULL REFERENCES seat_hold(id),
    show_seat_id UUID NOT NULL REFERENCES show_seat(id),
    PRIMARY KEY (hold_id, show_seat_id)
);

CREATE INDEX ix_seat_hold_item_show_seat_hold
    ON seat_hold_item (show_seat_id, hold_id);
