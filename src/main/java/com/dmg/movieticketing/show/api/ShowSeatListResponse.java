package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.application.ShowSeatListing;

import java.util.List;
import java.util.UUID;

public record ShowSeatListResponse(
        UUID showId,
        String currency,
        List<ShowSeatResponse> items
) {

    public static ShowSeatListResponse from(ShowSeatListing listing) {
        return new ShowSeatListResponse(
                listing.showId(),
                listing.currency(),
                listing.seats().stream().map(ShowSeatResponse::from).toList()
        );
    }
}
