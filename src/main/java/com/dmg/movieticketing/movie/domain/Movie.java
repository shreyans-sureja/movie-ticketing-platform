package com.dmg.movieticketing.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    private UUID id;

    @Column(name = "created_by_account_id", nullable = false)
    private UUID createdByAccountId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Movie() {
    }

    private Movie(
            UUID id,
            UUID createdByAccountId,
            String title,
            int durationMinutes,
            String languageCode,
            Instant createdAt
    ) {
        this.id = id;
        this.createdByAccountId = createdByAccountId;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.languageCode = languageCode;
        this.createdAt = createdAt;
    }

    public static Movie create(
            UUID id,
            UUID createdByAccountId,
            String title,
            int durationMinutes,
            String languageCode,
            Instant createdAt
    ) {
        return new Movie(id, createdByAccountId, title, durationMinutes, languageCode, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatedByAccountId() {
        return createdByAccountId;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
