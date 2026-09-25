package com.dmg.movieticketing.movie.api;

import com.dmg.movieticketing.identity.security.AccountPrincipal;
import com.dmg.movieticketing.movie.application.MovieCatalogService;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MovieCatalogService movieCatalogService;

    public MovieController(MovieCatalogService movieCatalogService) {
        this.movieCatalogService = movieCatalogService;
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody CreateMovieRequest request
    ) {
        MovieResponse response = MovieResponse.from(movieCatalogService.createMovie(
                principal.accountId(),
                request.title(),
                request.durationMinutes(),
                request.languageCode()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{movieId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public MovieListResponse searchMovies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String languageCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("title").ignoreCase(),
                        Sort.Order.asc("languageCode"),
                        Sort.Order.asc("durationMinutes"),
                        Sort.Order.asc("id")
                )
        );
        return MovieListResponse.from(movieCatalogService.searchMovies(q, languageCode, pageable)
                .map(MovieSummaryResponse::from));
    }

    @GetMapping("/{movieId}")
    public MovieResponse getMovie(@PathVariable UUID movieId) {
        return MovieResponse.from(movieCatalogService.getMovie(movieId));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new DomainValidationException("page", "PAGE_MIN", "Page must be zero or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException("size", "SIZE_RANGE", "Size must be between 1 and 100.");
        }
    }
}
