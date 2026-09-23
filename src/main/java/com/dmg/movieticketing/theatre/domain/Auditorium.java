package com.dmg.movieticketing.theatre.domain;

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
@Table(name = "auditorium")
public class Auditorium {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Auditorium() {
    }

    private Auditorium(UUID id, Theatre theatre, String name, Instant createdAt) {
        this.id = id;
        this.theatre = theatre;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static Auditorium create(UUID id, Theatre theatre, String name, Instant createdAt) {
        return new Auditorium(id, theatre, name, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Theatre getTheatre() {
        return theatre;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
