package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.application.ShowSearchItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowSearchItemResponse(
        UUID id,
        ShowMovieResponse movie,
        ShowTheatreResponse theatre,
        ShowAuditoriumResponse auditorium,
        Instant startsAt,
        Instant endsAt,
        String currency,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long availableSeatCount
) {

    public static ShowSearchItemResponse from(ShowSearchItem item) {
        return new ShowSearchItemResponse(
                item.showId(),
                new ShowMovieResponse(
                        item.movieId(),
                        item.movieTitle(),
                        item.movieDurationMinutes(),
                        item.movieLanguageCode()
                ),
                new ShowTheatreResponse(item.theatreId(), item.theatreName()),
                new ShowAuditoriumResponse(item.auditoriumId(), item.auditoriumName()),
                item.startsAt(),
                item.endsAt(),
                item.currency(),
                item.minimumPrice(),
                item.maximumPrice(),
                item.availableSeatCount()
        );
    }
}
