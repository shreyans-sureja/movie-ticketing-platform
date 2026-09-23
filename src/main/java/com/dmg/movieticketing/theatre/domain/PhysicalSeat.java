package com.dmg.movieticketing.theatre.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "physical_seat")
public class PhysicalSeat {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatTier tier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PhysicalSeat() {
    }

    private PhysicalSeat(
            UUID id,
            Auditorium auditorium,
            String rowLabel,
            int seatNumber,
            SeatTier tier,
            Instant createdAt
    ) {
        this.id = id;
        this.auditorium = auditorium;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.tier = tier;
        this.createdAt = createdAt;
    }

    public static PhysicalSeat create(
            UUID id,
            Auditorium auditorium,
            String rowLabel,
            int seatNumber,
            SeatTier tier,
            Instant createdAt
    ) {
        return new PhysicalSeat(id, auditorium, rowLabel, seatNumber, tier, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Auditorium getAuditorium() {
        return auditorium;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
