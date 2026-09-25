package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.application.ShowSeatView;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.UUID;

public record ShowSeatResponse(
        UUID showSeatId,
        String rowLabel,
        int seatNumber,
        String seatLabel,
        SeatTier tier,
        MoneyResponse price,
        ShowSeatAvailability availability
) {

    public static ShowSeatResponse from(ShowSeatView view) {
        var seat = view.seat();
        return new ShowSeatResponse(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getRowLabel() + seat.getSeatNumber(),
                seat.getTier(),
                new MoneyResponse(seat.getPrice(), seat.getCurrency()),
                view.availability()
        );
    }
}
