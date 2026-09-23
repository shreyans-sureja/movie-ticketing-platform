package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.identity.security.AccountPrincipal;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import com.dmg.movieticketing.theatre.application.TheatreManagementService;
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
@RequestMapping("/api/v1/theatres")
public class TheatreController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TheatreManagementService theatreManagementService;

    public TheatreController(TheatreManagementService theatreManagementService) {
        this.theatreManagementService = theatreManagementService;
    }

    @PostMapping
    public ResponseEntity<TheatreResponse> createTheatre(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody CreateTheatreRequest request
    ) {
        TheatreResponse response = TheatreResponse.from(theatreManagementService.createTheatre(
                principal.accountId(),
                request.cityId(),
                request.name(),
                request.addressLine1(),
                request.addressLine2(),
                request.postalCode()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{theatreId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public TheatreListResponse listOwnedTheatres(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );
        return TheatreListResponse.from(
                theatreManagementService.listOwnedTheatres(principal.accountId(), pageable)
                        .map(TheatreResponse::from)
        );
    }

    @PostMapping("/{theatreId}/auditoriums")
    public ResponseEntity<AuditoriumResponse> createAuditorium(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID theatreId,
            @Valid @RequestBody CreateAuditoriumRequest request
    ) {
        AuditoriumResponse response = AuditoriumResponse.from(theatreManagementService.createAuditorium(
                principal.accountId(),
                theatreId,
                request.name()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{auditoriumId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{theatreId}/auditoriums")
    public AuditoriumListResponse listOwnedAuditoriums(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID theatreId
    ) {
        return new AuditoriumListResponse(
                theatreManagementService.listOwnedAuditoriums(principal.accountId(), theatreId).stream()
                        .map(AuditoriumResponse::from)
                        .toList()
        );
    }

    @PostMapping("/{theatreId}/auditoriums/{auditoriumId}/seat-rows")
    public ResponseEntity<SeatRowResponse> createSeatRow(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID theatreId,
            @PathVariable UUID auditoriumId,
            @Valid @RequestBody CreateSeatRowRequest request
    ) {
        SeatRowResponse response = SeatRowResponse.from(theatreManagementService.createSeatRow(
                principal.accountId(),
                theatreId,
                auditoriumId,
                request.rowLabel(),
                request.firstSeatNumber(),
                request.seatCount(),
                request.tier()
        ));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
                .body(response);
    }

    @GetMapping("/{theatreId}/auditoriums/{auditoriumId}/seats")
    public PhysicalSeatListResponse listOwnedSeats(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID theatreId,
            @PathVariable UUID auditoriumId
    ) {
        return new PhysicalSeatListResponse(
                theatreManagementService.listOwnedSeats(principal.accountId(), theatreId, auditoriumId).stream()
                        .map(PhysicalSeatResponse::from)
                        .toList()
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new DomainValidationException("page", "PAGE_MIN", "Page must be zero or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException(
                    "size",
                    "SIZE_RANGE",
                    "Size must be between 1 and 100."
            );
        }
    }
}
