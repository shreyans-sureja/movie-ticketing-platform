package com.dmg.movieticketing.movie.api;

import com.dmg.movieticketing.movie.domain.Movie;

import java.util.UUID;

public record MovieSummaryResponse(
        UUID id,
        String title,
        int durationMinutes,
        String languageCode
) {

    public static MovieSummaryResponse from(Movie movie) {
        return new MovieSummaryResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getLanguageCode()
        );
    }
}
