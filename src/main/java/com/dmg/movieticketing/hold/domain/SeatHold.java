package com.dmg.movieticketing.hold.domain;

import com.dmg.movieticketing.show.domain.MovieShow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seat_hold")
public class SeatHold {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private MovieShow show;

    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected SeatHold() {
    }

    private SeatHold(
            UUID id,
            MovieShow show,
            UUID customerAccountId,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.show = show;
        this.customerAccountId = customerAccountId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static SeatHold create(
            UUID id,
            MovieShow show,
            UUID customerAccountId,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new SeatHold(id, show, customerAccountId, createdAt, expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public MovieShow getShow() {
        return show;
    }

    public UUID getCustomerAccountId() {
        return customerAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActiveAt(Instant instant) {
        return instant.isBefore(expiresAt);
    }
}
