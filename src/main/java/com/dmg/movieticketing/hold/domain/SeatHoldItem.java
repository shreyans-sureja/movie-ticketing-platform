package com.dmg.movieticketing.hold.domain;

import com.dmg.movieticketing.show.domain.ShowSeat;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "seat_hold_item")
public class SeatHoldItem {

    @EmbeddedId
    private SeatHoldItemId id;

    @MapsId("holdId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hold_id", nullable = false)
    private SeatHold hold;

    @MapsId("showSeatId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    protected SeatHoldItem() {
    }

    private SeatHoldItem(SeatHold hold, ShowSeat showSeat) {
        this.id = new SeatHoldItemId(hold.getId(), showSeat.getId());
        this.hold = hold;
        this.showSeat = showSeat;
    }

    public static SeatHoldItem create(SeatHold hold, ShowSeat showSeat) {
        return new SeatHoldItem(hold, showSeat);
    }

    public SeatHold getHold() {
        return hold;
    }

    public ShowSeat getShowSeat() {
        return showSeat;
    }
}
