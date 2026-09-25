package com.dmg.movieticketing.booking.api;

import com.dmg.movieticketing.booking.domain.BookingItem;
import com.dmg.movieticketing.show.api.MoneyResponse;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.util.UUID;

public record BookingSeatResponse(
        UUID showSeatId,
        String rowLabel,
        int seatNumber,
        String seatLabel,
        SeatTier tier,
        MoneyResponse price
) {

    static BookingSeatResponse from(BookingItem item, String currency) {
        var seat = item.getShowSeat();
        return new BookingSeatResponse(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getRowLabel() + seat.getSeatNumber(),
                seat.getTier(),
                new MoneyResponse(item.getUnitPrice(), currency)
        );
    }
}
