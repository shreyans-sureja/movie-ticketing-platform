package com.dmg.movieticketing.theatre.application;

import com.dmg.movieticketing.theatre.domain.PhysicalSeat;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.List;
import java.util.UUID;

public record SeatRowCreation(
        UUID auditoriumId,
        String rowLabel,
        int firstSeatNumber,
        int seatCount,
        SeatTier tier,
        List<PhysicalSeat> seats
) {
}
