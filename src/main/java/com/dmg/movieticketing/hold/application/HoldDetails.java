package com.dmg.movieticketing.hold.application;

import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.show.domain.ShowSeat;

import java.util.List;

public record HoldDetails(
        SeatHold hold,
        HoldStatus status,
        List<ShowSeat> seats
) {
}
