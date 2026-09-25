package com.dmg.movieticketing.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BookingItemId implements Serializable {

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "show_seat_id")
    private UUID showSeatId;

    protected BookingItemId() {
    }

    public BookingItemId(UUID bookingId, UUID showSeatId) {
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BookingItemId that)) {
            return false;
        }
        return Objects.equals(bookingId, that.bookingId)
                && Objects.equals(showSeatId, that.showSeatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, showSeatId);
    }
}
