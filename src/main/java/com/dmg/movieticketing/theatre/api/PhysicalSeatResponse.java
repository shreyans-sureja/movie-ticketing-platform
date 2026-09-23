package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.theatre.domain.PhysicalSeat;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.UUID;

public record PhysicalSeatResponse(
        UUID id,
        String rowLabel,
        int seatNumber,
        String seatLabel,
        SeatTier tier
) {

    public static PhysicalSeatResponse from(PhysicalSeat seat) {
        return new PhysicalSeatResponse(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getRowLabel() + seat.getSeatNumber(),
                seat.getTier()
        );
    }
}
