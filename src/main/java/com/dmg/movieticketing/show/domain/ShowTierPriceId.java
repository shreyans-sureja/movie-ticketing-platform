package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.theatre.domain.SeatTier;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ShowTierPriceId implements Serializable {

    @Column(name = "show_id")
    private UUID showId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    private SeatTier tier;

    protected ShowTierPriceId() {
    }

    public ShowTierPriceId(UUID showId, SeatTier tier) {
        this.showId = showId;
        this.tier = tier;
    }

    public UUID getShowId() {
        return showId;
    }

    public SeatTier getTier() {
        return tier;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ShowTierPriceId that)) {
            return false;
        }
        return Objects.equals(showId, that.showId) && tier == that.tier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showId, tier);
    }
}
