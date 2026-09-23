package com.dmg.movieticketing.theatre.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TheatreRepository extends JpaRepository<Theatre, UUID> {

    @EntityGraph(attributePaths = "city")
    Page<Theatre> findAllByOwnerAccountId(UUID ownerAccountId, Pageable pageable);

    @EntityGraph(attributePaths = "city")
    Optional<Theatre> findByIdAndOwnerAccountId(UUID id, UUID ownerAccountId);

    boolean existsByOwnerAccountIdAndCityIdAndNameIgnoreCase(
            UUID ownerAccountId,
            Long cityId,
            String name
    );
}
