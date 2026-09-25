package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.identity.security.AccountPrincipal;
import com.dmg.movieticketing.show.application.ShowService;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
public class ShowController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping("/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/shows")
    public ResponseEntity<ShowResponse> createShow(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID theatreId,
            @PathVariable UUID auditoriumId,
            @Valid @RequestBody CreateShowRequest request
    ) {
        ShowResponse response = ShowResponse.from(showService.createShow(
                principal.accountId(),
                theatreId,
                auditoriumId,
                request.movieId(),
                request.startsAt().toInstant(),
                request.currency(),
                request.tierPrices().stream().map(TierPriceRequest::toInput).toList()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/shows/{showId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/shows")
    public ShowListResponse searchShows(
            @RequestParam long cityId,
            @RequestParam UUID movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        return ShowListResponse.from(showService.searchShows(
                cityId,
                movieId,
                date,
                PageRequest.of(page, size)
        ).map(ShowSearchItemResponse::from));
    }

    @GetMapping("/api/v1/shows/{showId}")
    public ShowResponse getShow(@PathVariable UUID showId) {
        return ShowResponse.from(showService.getShow(showId));
    }

    @GetMapping("/api/v1/shows/{showId}/seats")
    public ShowSeatListResponse listShowSeats(@PathVariable UUID showId) {
        return ShowSeatListResponse.from(showService.listShowSeats(showId));
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
