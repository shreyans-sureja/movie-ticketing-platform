package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.theatre.domain.PhysicalSeat;
import com.dmg.movieticketing.theatre.domain.SeatTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "show_seat")
public class ShowSeat {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private MovieShow show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "physical_seat_id", nullable = false)
    private PhysicalSeat physicalSeat;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatTier tier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private ShowSeatAvailability availabilityStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShowSeat() {
    }

    private ShowSeat(
            UUID id,
            MovieShow show,
            PhysicalSeat physicalSeat,
            String rowLabel,
            int seatNumber,
            SeatTier tier,
            BigDecimal price,
            String currency,
            Instant createdAt
    ) {
        this.id = id;
        this.show = show;
        this.physicalSeat = physicalSeat;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.tier = tier;
        this.price = price;
        this.currency = currency;
        this.availabilityStatus = ShowSeatAvailability.AVAILABLE;
        this.createdAt = createdAt;
    }

    public static ShowSeat create(
            UUID id,
            MovieShow show,
            PhysicalSeat physicalSeat,
            BigDecimal price,
            String currency,
            Instant createdAt
    ) {
        return new ShowSeat(
                id,
                show,
                physicalSeat,
                physicalSeat.getRowLabel(),
                physicalSeat.getSeatNumber(),
                physicalSeat.getTier(),
                price,
                currency,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public MovieShow getShow() {
        return show;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatTier getTier() {
        return tier;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public ShowSeatAvailability getAvailabilityStatus() {
        return availabilityStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
