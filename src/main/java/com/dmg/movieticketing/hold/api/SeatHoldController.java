package com.dmg.movieticketing.hold.api;

import com.dmg.movieticketing.hold.application.SeatHoldService;
import com.dmg.movieticketing.identity.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/api/v1/shows/{showId}/holds")
    public ResponseEntity<HoldResponse> createHold(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID showId,
            @Valid @RequestBody CreateHoldRequest request
    ) {
        HoldResponse response = HoldResponse.from(seatHoldService.createHold(
                principal.accountId(),
                showId,
                request.showSeatIds()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/holds/{holdId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/holds/{holdId}")
    public HoldResponse getHold(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID holdId
    ) {
        return HoldResponse.from(seatHoldService.getHold(principal.accountId(), holdId));
    }
}
