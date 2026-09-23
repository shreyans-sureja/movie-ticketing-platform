package com.dmg.movieticketing.theatre.domain;

import com.dmg.movieticketing.city.domain.City;
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
@Table(name = "theatre")
public class Theatre {

    @Id
    private UUID id;

    @Column(name = "owner_account_id", nullable = false)
    private UUID ownerAccountId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "address_line_1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line_2", length = 200)
    private String addressLine2;

    @Column(name = "postal_code", nullable = false, length = 6)
    private String postalCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Theatre() {
    }

    private Theatre(
            UUID id,
            UUID ownerAccountId,
            City city,
            String name,
            String addressLine1,
            String addressLine2,
            String postalCode,
            Instant createdAt
    ) {
        this.id = id;
        this.ownerAccountId = ownerAccountId;
        this.city = city;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.postalCode = postalCode;
        this.createdAt = createdAt;
    }

    public static Theatre create(
            UUID id,
            UUID ownerAccountId,
            City city,
            String name,
            String addressLine1,
            String addressLine2,
            String postalCode,
            Instant createdAt
    ) {
        return new Theatre(id, ownerAccountId, city, name, addressLine1, addressLine2, postalCode, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerAccountId() {
        return ownerAccountId;
    }

    public City getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
