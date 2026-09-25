package com.dmg.movieticketing.booking.domain;

import com.dmg.movieticketing.show.domain.ShowSeat;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_item")
public class BookingItem {

    @EmbeddedId
    private BookingItemId id;

    @MapsId("bookingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @MapsId("showSeatId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected BookingItem() {
    }

    private BookingItem(Booking booking, ShowSeat showSeat) {
        this.id = new BookingItemId(booking.getId(), showSeat.getId());
        this.booking = booking;
        this.showSeat = showSeat;
        this.unitPrice = showSeat.getPrice();
    }

    public static BookingItem create(Booking booking, ShowSeat showSeat) {
        return new BookingItem(booking, showSeat);
    }

    public Booking getBooking() {
        return booking;
    }

    public ShowSeat getShowSeat() {
        return showSeat;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
