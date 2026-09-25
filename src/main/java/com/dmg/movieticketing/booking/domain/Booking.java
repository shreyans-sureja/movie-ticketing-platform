package com.dmg.movieticketing.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    private UUID id;

    @Column(name = "source_hold_id", nullable = false)
    private UUID sourceHoldId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    protected Booking() {
    }

    private Booking(
            UUID id,
            UUID sourceHoldId,
            UUID showId,
            UUID customerAccountId,
            BigDecimal totalAmount,
            String currency,
            Instant confirmedAt
    ) {
        this.id = id;
        this.sourceHoldId = sourceHoldId;
        this.showId = showId;
        this.customerAccountId = customerAccountId;
        this.status = BookingStatus.CONFIRMED;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.confirmedAt = confirmedAt;
    }

    public static Booking confirm(
            UUID id,
            UUID sourceHoldId,
            UUID showId,
            UUID customerAccountId,
            BigDecimal totalAmount,
            String currency,
            Instant confirmedAt
    ) {
        return new Booking(
                id,
                sourceHoldId,
                showId,
                customerAccountId,
                totalAmount,
                currency,
                confirmedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceHoldId() {
        return sourceHoldId;
    }

    public UUID getShowId() {
        return showId;
    }

    public UUID getCustomerAccountId() {
        return customerAccountId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
