package com.dmg.movieticketing.hold.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SeatHoldItemId implements Serializable {

    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "show_seat_id")
    private UUID showSeatId;

    protected SeatHoldItemId() {
    }

    public SeatHoldItemId(UUID holdId, UUID showSeatId) {
        this.holdId = holdId;
        this.showSeatId = showSeatId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SeatHoldItemId that)) {
            return false;
        }
        return Objects.equals(holdId, that.holdId) && Objects.equals(showSeatId, that.showSeatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holdId, showSeatId);
    }
}
