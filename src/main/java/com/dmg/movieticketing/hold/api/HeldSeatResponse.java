package com.dmg.movieticketing.hold.api;

import com.dmg.movieticketing.show.api.MoneyResponse;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.UUID;

public record HeldSeatResponse(
        UUID showSeatId,
        String rowLabel,
        int seatNumber,
        String seatLabel,
        SeatTier tier,
        MoneyResponse price
) {

    static HeldSeatResponse from(ShowSeat seat) {
        return new HeldSeatResponse(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getRowLabel() + seat.getSeatNumber(),
                seat.getTier(),
                new MoneyResponse(seat.getPrice(), seat.getCurrency())
        );
    }
}
