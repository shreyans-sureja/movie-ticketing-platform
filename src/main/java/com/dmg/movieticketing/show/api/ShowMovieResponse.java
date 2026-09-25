package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.movie.domain.Movie;

import java.util.UUID;

public record ShowMovieResponse(
        UUID id,
        String title,
        int durationMinutes,
        String languageCode
) {

    public static ShowMovieResponse from(Movie movie) {
        return new ShowMovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getLanguageCode()
        );
    }
}
