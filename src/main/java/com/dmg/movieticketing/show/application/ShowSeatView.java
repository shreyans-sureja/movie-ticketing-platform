package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;

public record ShowSeatView(
        ShowSeat seat,
        ShowSeatAvailability availability
) {
}
