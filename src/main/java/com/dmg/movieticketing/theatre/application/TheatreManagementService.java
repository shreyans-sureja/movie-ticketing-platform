package com.dmg.movieticketing.theatre.application;

import com.dmg.movieticketing.city.application.CityNotFoundException;
import com.dmg.movieticketing.city.domain.City;
import com.dmg.movieticketing.city.domain.CityRepository;
import com.dmg.movieticketing.theatre.domain.Auditorium;
import com.dmg.movieticketing.theatre.domain.AuditoriumRepository;
import com.dmg.movieticketing.theatre.domain.PhysicalSeat;
import com.dmg.movieticketing.theatre.domain.PhysicalSeatRepository;
import com.dmg.movieticketing.theatre.domain.SeatTier;
import com.dmg.movieticketing.theatre.domain.Theatre;
import com.dmg.movieticketing.theatre.domain.TheatreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TheatreManagementService {

    private static final int MAX_SEAT_NUMBER = 999;

    private final CityRepository cityRepository;
    private final TheatreRepository theatreRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final PhysicalSeatRepository physicalSeatRepository;
    private final Clock clock;

    public TheatreManagementService(
            CityRepository cityRepository,
            TheatreRepository theatreRepository,
            AuditoriumRepository auditoriumRepository,
            PhysicalSeatRepository physicalSeatRepository,
            Clock clock
    ) {
        this.cityRepository = cityRepository;
        this.theatreRepository = theatreRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.physicalSeatRepository = physicalSeatRepository;
        this.clock = clock;
    }

    @Transactional
    public Theatre createTheatre(
            UUID accountId,
            long cityId,
            String name,
            String addressLine1,
            String addressLine2,
            String postalCode
    ) {
        City city = cityRepository.findById(cityId).orElseThrow(CityNotFoundException::new);
        String normalizedName = name.trim();

        if (theatreRepository.existsByOwnerAccountIdAndCityIdAndNameIgnoreCase(
                accountId,
                cityId,
                normalizedName
        )) {
            throw new TheatreNameConflictException();
        }

        Theatre theatre = Theatre.create(
                UUID.randomUUID(),
                accountId,
                city,
                normalizedName,
                addressLine1.trim(),
                nullableTrim(addressLine2),
                postalCode,
                clock.instant()
        );

        try {
            return theatreRepository.saveAndFlush(theatre);
        } catch (DataIntegrityViolationException exception) {
            throw new TheatreNameConflictException(exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<Theatre> listOwnedTheatres(UUID accountId, Pageable pageable) {
        return theatreRepository.findAllByOwnerAccountId(accountId, pageable);
    }

    @Transactional
    public AuditoriumDetails createAuditorium(UUID accountId, UUID theatreId, String name) {
        Theatre theatre = ownedTheatre(theatreId, accountId);
        String normalizedName = name.trim();

        if (auditoriumRepository.existsByTheatreIdAndNameIgnoreCase(theatreId, normalizedName)) {
            throw new AuditoriumNameConflictException();
        }

        Auditorium auditorium = Auditorium.create(
                UUID.randomUUID(),
                theatre,
                normalizedName,
                clock.instant()
        );

        try {
            Auditorium saved = auditoriumRepository.saveAndFlush(auditorium);
            return toDetails(saved, 0);
        } catch (DataIntegrityViolationException exception) {
            throw new AuditoriumNameConflictException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditoriumDetails> listOwnedAuditoriums(UUID accountId, UUID theatreId) {
        ownedTheatre(theatreId, accountId);
        return auditoriumRepository.findAllByTheatreIdOrderByNameAscIdAsc(theatreId).stream()
                .map(auditorium -> toDetails(
                        auditorium,
                        physicalSeatRepository.countByAuditoriumId(auditorium.getId())
                ))
                .toList();
    }

    @Transactional
    public SeatRowCreation createSeatRow(
            UUID accountId,
            UUID theatreId,
            UUID auditoriumId,
            String rowLabel,
            int firstSeatNumber,
            int seatCount,
            SeatTier tier
    ) {
        Auditorium auditorium = ownedAuditorium(accountId, theatreId, auditoriumId);
        String normalizedRowLabel = rowLabel.trim().toUpperCase(Locale.ROOT);
        int lastSeatNumber = calculateLastSeatNumber(firstSeatNumber, seatCount);

        if (physicalSeatRepository.existsByAuditoriumIdAndRowLabelAndSeatNumberBetween(
                auditoriumId,
                normalizedRowLabel,
                firstSeatNumber,
                lastSeatNumber
        )) {
            throw new SeatConflictException();
        }

        Instant now = clock.instant();
        List<PhysicalSeat> seats = new ArrayList<>(seatCount);
        for (int seatNumber = firstSeatNumber; seatNumber <= lastSeatNumber; seatNumber++) {
            seats.add(PhysicalSeat.create(
                    UUID.randomUUID(),
                    auditorium,
                    normalizedRowLabel,
                    seatNumber,
                    tier,
                    now
            ));
        }

        try {
            List<PhysicalSeat> saved = physicalSeatRepository.saveAllAndFlush(seats);
            return new SeatRowCreation(
                    auditoriumId,
                    normalizedRowLabel,
                    firstSeatNumber,
                    seatCount,
                    tier,
                    List.copyOf(saved)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new SeatConflictException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<PhysicalSeat> listOwnedSeats(UUID accountId, UUID theatreId, UUID auditoriumId) {
        ownedAuditorium(accountId, theatreId, auditoriumId);
        return physicalSeatRepository.findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAscIdAsc(auditoriumId);
    }

    private Theatre ownedTheatre(UUID theatreId, UUID accountId) {
        return theatreRepository.findByIdAndOwnerAccountId(theatreId, accountId)
                .orElseThrow(TheatreNotFoundException::new);
    }

    private Auditorium ownedAuditorium(UUID accountId, UUID theatreId, UUID auditoriumId) {
        ownedTheatre(theatreId, accountId);
        return auditoriumRepository.findByIdAndTheatreId(auditoriumId, theatreId)
                .orElseThrow(AuditoriumNotFoundException::new);
    }

    private int calculateLastSeatNumber(int firstSeatNumber, int seatCount) {
        long lastSeatNumber = (long) firstSeatNumber + seatCount - 1;
        if (lastSeatNumber > MAX_SEAT_NUMBER) {
            throw new DomainValidationException(
                    "seatCount",
                    "SEAT_RANGE",
                    "The generated seat range must end at seat number 999 or lower."
            );
        }
        return (int) lastSeatNumber;
    }

    private AuditoriumDetails toDetails(Auditorium auditorium, long seatCount) {
        return new AuditoriumDetails(
                auditorium.getId(),
                auditorium.getTheatre().getId(),
                auditorium.getName(),
                seatCount,
                auditorium.getCreatedAt()
        );
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
