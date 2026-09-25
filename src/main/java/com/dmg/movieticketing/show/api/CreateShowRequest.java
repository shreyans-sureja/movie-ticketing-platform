package com.dmg.movieticketing.show.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateShowRequest(
        @NotNull UUID movieId,
        @NotNull OffsetDateTime startsAt,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotEmpty List<@Valid TierPriceRequest> tierPrices
) {
}
