package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.theatre.domain.SeatTier;

import java.math.BigDecimal;

public record TierPriceInput(SeatTier tier, BigDecimal amount) {
}
