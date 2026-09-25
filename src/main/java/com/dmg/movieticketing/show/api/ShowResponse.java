package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.application.ShowDetails;
import com.dmg.movieticketing.show.domain.MovieShow;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShowResponse(
        UUID id,
        ShowMovieResponse movie,
        ShowTheatreResponse theatre,
        ShowAuditoriumResponse auditorium,
        Instant startsAt,
        Instant endsAt,
        String currency,
        List<TierPriceResponse> tierPrices,
        long totalSeatCount,
        long availableSeatCount,
        Instant createdAt
) {

    public static ShowResponse from(ShowDetails details) {
        MovieShow show = details.show();
        return new ShowResponse(
                show.getId(),
                ShowMovieResponse.from(show.getMovie()),
                ShowTheatreResponse.from(show.getAuditorium().getTheatre()),
                ShowAuditoriumResponse.from(show.getAuditorium()),
                show.getStartsAt(),
                show.getEndsAt(),
                show.getCurrency(),
                details.tierPrices().stream().map(TierPriceResponse::from).toList(),
                details.totalSeatCount(),
                details.availableSeatCount(),
                show.getCreatedAt()
        );
    }
}
