package com.dmg.movieticketing.movie.application;

import com.dmg.movieticketing.movie.domain.Movie;
import com.dmg.movieticketing.movie.domain.MovieRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
public class MovieCatalogService {

    private final MovieRepository movieRepository;
    private final Clock clock;

    public MovieCatalogService(MovieRepository movieRepository, Clock clock) {
        this.movieRepository = movieRepository;
        this.clock = clock;
    }

    @Transactional
    public Movie createMovie(UUID accountId, String title, int durationMinutes, String languageCode) {
        String normalizedTitle = normalizeTitle(title);
        String normalizedLanguage = languageCode.trim().toLowerCase(Locale.ROOT);

        if (movieRepository.existsByTitleIgnoreCaseAndLanguageCodeIgnoreCaseAndDurationMinutes(
                normalizedTitle,
                normalizedLanguage,
                durationMinutes
        )) {
            throw new MovieAlreadyExistsException();
        }

        Movie movie = Movie.create(
                UUID.randomUUID(),
                accountId,
                normalizedTitle,
                durationMinutes,
                normalizedLanguage,
                clock.instant()
        );

        try {
            return movieRepository.saveAndFlush(movie);
        } catch (DataIntegrityViolationException exception) {
            throw new MovieAlreadyExistsException(exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<Movie> searchMovies(String query, String languageCode, Pageable pageable) {
        return movieRepository.search(nullableTrim(query), normalizeNullableLanguage(languageCode), pageable);
    }

    @Transactional(readOnly = true)
    public Movie getMovie(UUID movieId) {
        return movieRepository.findById(movieId).orElseThrow(MovieNotFoundException::new);
    }

    private String normalizeTitle(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullableLanguage(String value) {
        String trimmed = nullableTrim(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
