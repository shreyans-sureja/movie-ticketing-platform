package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.theatre.application.SeatRowCreation;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.List;
import java.util.UUID;

public record SeatRowResponse(
        UUID auditoriumId,
        String rowLabel,
        int firstSeatNumber,
        int seatCount,
        SeatTier tier,
        List<PhysicalSeatResponse> seats
) {

    public static SeatRowResponse from(SeatRowCreation creation) {
        return new SeatRowResponse(
                creation.auditoriumId(),
                creation.rowLabel(),
                creation.firstSeatNumber(),
                creation.seatCount(),
                creation.tier(),
                creation.seats().stream().map(PhysicalSeatResponse::from).toList()
        );
    }
}
