package com.dmg.movieticketing.show.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShowTierPriceRepository extends JpaRepository<ShowTierPrice, ShowTierPriceId> {

    @Query("""
            SELECT tierPrice
            FROM ShowTierPrice tierPrice
            WHERE tierPrice.show.id = :showId
            ORDER BY tierPrice.id.tier ASC
            """)
    List<ShowTierPrice> findAllByShowIdOrderByTierAsc(@Param("showId") UUID showId);
}
