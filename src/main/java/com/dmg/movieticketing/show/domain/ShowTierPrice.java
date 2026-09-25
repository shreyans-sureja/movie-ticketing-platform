package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.theatre.domain.SeatTier;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "show_tier_price")
public class ShowTierPrice {

    @EmbeddedId
    private ShowTierPriceId id;

    @MapsId("showId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private MovieShow show;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    protected ShowTierPrice() {
    }

    private ShowTierPrice(MovieShow show, SeatTier tier, BigDecimal price, String currency) {
        this.id = new ShowTierPriceId(show.getId(), tier);
        this.show = show;
        this.price = price;
        this.currency = currency;
    }

    public static ShowTierPrice create(MovieShow show, SeatTier tier, BigDecimal price, String currency) {
        return new ShowTierPrice(show, tier, price, currency);
    }

    public MovieShow getShow() {
        return show;
    }

    public SeatTier getTier() {
        return id.getTier();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
