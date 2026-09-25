package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.application.TierPriceInput;
import com.dmg.movieticketing.theatre.domain.SeatTier;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TierPriceRequest(
        @NotNull SeatTier tier,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount
) {

    public TierPriceInput toInput() {
        return new TierPriceInput(tier, amount);
    }
}
