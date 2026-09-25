package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.movie.domain.Movie;
import com.dmg.movieticketing.theatre.domain.Auditorium;
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
@Table(name = "movie_show")
public class MovieShow {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(name = "scheduled_by_account_id", nullable = false)
    private UUID scheduledByAccountId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MovieShow() {
    }

    private MovieShow(
            UUID id,
            Movie movie,
            Auditorium auditorium,
            UUID scheduledByAccountId,
            Instant startsAt,
            Instant endsAt,
            String currency,
            Instant createdAt
    ) {
        this.id = id;
        this.movie = movie;
        this.auditorium = auditorium;
        this.scheduledByAccountId = scheduledByAccountId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public static MovieShow create(
            UUID id,
            Movie movie,
            Auditorium auditorium,
            UUID scheduledByAccountId,
            Instant startsAt,
            Instant endsAt,
            String currency,
            Instant createdAt
    ) {
        return new MovieShow(
                id,
                movie,
                auditorium,
                scheduledByAccountId,
                startsAt,
                endsAt,
                currency,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public UUID getScheduledByAccountId() {
        return scheduledByAccountId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
