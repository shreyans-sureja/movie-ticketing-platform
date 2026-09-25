package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.show.domain.ShowTierPrice;
import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.math.BigDecimal;

public record TierPriceResponse(SeatTier tier, BigDecimal amount) {

    public static TierPriceResponse from(ShowTierPrice tierPrice) {
        return new TierPriceResponse(tierPrice.getTier(), tierPrice.getPrice());
    }
}
