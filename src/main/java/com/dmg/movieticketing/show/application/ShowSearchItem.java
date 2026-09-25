package com.dmg.movieticketing.show.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowSearchItem(
        UUID showId,
        UUID movieId,
        String movieTitle,
        int movieDurationMinutes,
        String movieLanguageCode,
        UUID theatreId,
        String theatreName,
        UUID auditoriumId,
        String auditoriumName,
        Instant startsAt,
        Instant endsAt,
        String currency,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long availableSeatCount
) {
}
