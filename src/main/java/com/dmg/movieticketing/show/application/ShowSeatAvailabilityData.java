package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;

import java.time.Instant;

public record ShowSeatAvailabilityData(
        ShowSeat seat,
        Instant currentHoldExpiresAt
) {

    public ShowSeatAvailability effectiveAvailabilityAt(Instant requestNow) {
        if (seat.getAvailabilityStatus() == ShowSeatAvailability.AVAILABLE) {
            return ShowSeatAvailability.AVAILABLE;
        }
        if (currentHoldExpiresAt != null && !currentHoldExpiresAt.isAfter(requestNow)) {
            return ShowSeatAvailability.AVAILABLE;
        }
        return ShowSeatAvailability.HELD;
    }
}
