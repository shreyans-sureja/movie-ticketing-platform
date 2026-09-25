package com.dmg.movieticketing.hold.api;

import com.dmg.movieticketing.hold.application.HoldDetails;
import com.dmg.movieticketing.hold.application.HoldStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HoldResponse(
        UUID id,
        UUID showId,
        Instant createdAt,
        Instant expiresAt,
        HoldStatus status,
        List<HeldSeatResponse> seats
) {

    static HoldResponse from(HoldDetails details) {
        return new HoldResponse(
                details.hold().getId(),
                details.hold().getShow().getId(),
                details.hold().getCreatedAt(),
                details.hold().getExpiresAt(),
                details.status(),
                details.seats().stream().map(HeldSeatResponse::from).toList()
        );
    }
}
