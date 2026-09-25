package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.show.domain.ShowSeat;

import java.util.List;
import java.util.UUID;

public record ShowSeatListing(
        UUID showId,
        String currency,
        List<ShowSeat> seats
) {
}
