ALTER TABLE booking
    ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE booking
    DROP CONSTRAINT ck_booking_status,
    ADD CONSTRAINT ck_booking_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    ADD CONSTRAINT ck_booking_status_timestamp
        CHECK (
            (status = 'CONFIRMED' AND cancelled_at IS NULL)
            OR
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
        ),
    ADD CONSTRAINT ck_booking_cancelled_after_confirmed
        CHECK (cancelled_at IS NULL OR cancelled_at >= confirmed_at);
