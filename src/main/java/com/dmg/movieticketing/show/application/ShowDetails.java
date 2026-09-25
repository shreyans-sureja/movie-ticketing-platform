package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.show.domain.MovieShow;
import com.dmg.movieticketing.show.domain.ShowTierPrice;

import java.util.List;

public record ShowDetails(
        MovieShow show,
        List<ShowTierPrice> tierPrices,
        long totalSeatCount,
        long availableSeatCount
) {
}
