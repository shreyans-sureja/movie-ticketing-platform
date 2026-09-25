package com.dmg.movieticketing.movie.api;

import com.dmg.movieticketing.movie.domain.Movie;

import java.time.Instant;
import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        int durationMinutes,
        String languageCode,
        Instant createdAt
) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getLanguageCode(),
                movie.getCreatedAt()
        );
    }
}
