package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.theatre.domain.SeatTier;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSeatRowRequest(
        @NotNull @Pattern(regexp = "[A-Za-z]{1,10}") String rowLabel,
        @Min(1) @Max(999) int firstSeatNumber,
        @Min(1) @Max(500) int seatCount,
        @NotNull SeatTier tier
) {
}
