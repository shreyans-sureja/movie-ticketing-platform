package com.dmg.movieticketing.hold.application;

import com.dmg.movieticketing.hold.config.HoldProperties;
import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.hold.domain.SeatHoldItem;
import com.dmg.movieticketing.hold.domain.SeatHoldItemRepository;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.show.application.ShowNotFoundException;
import com.dmg.movieticketing.show.domain.MovieShow;
import com.dmg.movieticketing.show.domain.MovieShowRepository;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeatHoldService {

    private final MovieShowRepository movieShowRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final HoldProperties holdProperties;
    private final Clock clock;
    private final HoldConversionLookup holdConversionLookup;

    public SeatHoldService(
            MovieShowRepository movieShowRepository,
            ShowSeatRepository showSeatRepository,
            SeatHoldRepository seatHoldRepository,
            SeatHoldItemRepository seatHoldItemRepository,
            HoldProperties holdProperties,
            Clock clock,
            HoldConversionLookup holdConversionLookup
    ) {
        this.movieShowRepository = movieShowRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.seatHoldItemRepository = seatHoldItemRepository;
        this.holdProperties = holdProperties;
        this.clock = clock;
        this.holdConversionLookup = holdConversionLookup;
    }

    @Transactional
    public HoldDetails createHold(UUID customerAccountId, UUID showId, List<UUID> requestedShowSeatIds) {
        List<UUID> showSeatIds = validateAndSortSeatIds(requestedShowSeatIds);
        MovieShow show = movieShowRepository.findById(showId).orElseThrow(ShowNotFoundException::new);

        List<ShowSeat> lockedSeats = showSeatRepository.findAllForHoldUpdate(showId, showSeatIds);
        Instant acquiredAt = clock.instant();

        if (!show.getStartsAt().isAfter(acquiredAt)) {
            throw new ShowAlreadyStartedException();
        }
        if (lockedSeats.size() != showSeatIds.size()) {
            throw new SeatsUnavailableException();
        }

        Map<UUID, SeatHold> currentHolds = loadCurrentHolds(lockedSeats);
        boolean allAvailable = lockedSeats.stream()
                .allMatch(seat -> isEffectivelyAvailable(seat, currentHolds, acquiredAt));
        if (!allAvailable) {
            throw new SeatsUnavailableException();
        }

        SeatHold hold = SeatHold.create(
                UUID.randomUUID(),
                show,
                customerAccountId,
                acquiredAt,
                acquiredAt.plus(holdProperties.holdDuration())
        );
        seatHoldRepository.save(hold);

        List<SeatHoldItem> items = lockedSeats.stream()
                .map(seat -> SeatHoldItem.create(hold, seat))
                .toList();
        seatHoldItemRepository.saveAll(items);
        lockedSeats.forEach(seat -> seat.assignHold(hold.getId()));
        showSeatRepository.saveAllAndFlush(lockedSeats);

        return new HoldDetails(hold, HoldStatus.ACTIVE, orderForResponse(lockedSeats));
    }

    @Transactional(readOnly = true)
    public HoldDetails getHold(UUID customerAccountId, UUID holdId) {
        SeatHold hold = seatHoldRepository.findByIdAndCustomerAccountId(holdId, customerAccountId)
                .orElseThrow(HoldNotFoundException::new);
        List<ShowSeat> seats = seatHoldItemRepository.findAllForHold(holdId).stream()
                .map(SeatHoldItem::getShowSeat)
                .toList();
        HoldStatus status;
        if (holdConversionLookup.isConverted(holdId)) {
            status = HoldStatus.CONVERTED;
        } else {
            status = hold.isActiveAt(clock.instant()) ? HoldStatus.ACTIVE : HoldStatus.EXPIRED;
        }
        return new HoldDetails(hold, status, List.copyOf(seats));
    }

    private List<UUID> validateAndSortSeatIds(List<UUID> requestedShowSeatIds) {
        if (requestedShowSeatIds == null || requestedShowSeatIds.isEmpty()) {
            throw new DomainValidationException(
                    "showSeatIds",
                    "SHOW_SEAT_IDS_NOT_EMPTY",
                    "At least one show seat is required."
            );
        }
        if (requestedShowSeatIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DomainValidationException(
                    "showSeatIds",
                    "SHOW_SEAT_IDS_NOT_NULL",
                    "Show seat IDs must not contain null values."
            );
        }

        Set<UUID> distinctIds = new HashSet<>(requestedShowSeatIds);
        if (distinctIds.size() != requestedShowSeatIds.size()) {
            throw new DomainValidationException(
                    "showSeatIds",
                    "SHOW_SEAT_IDS_UNIQUE",
                    "Show seat IDs must not contain duplicates."
            );
        }
        return distinctIds.stream().sorted().toList();
    }

    private Map<UUID, SeatHold> loadCurrentHolds(List<ShowSeat> seats) {
        Set<UUID> holdIds = seats.stream()
                .filter(seat -> seat.getAvailabilityStatus() == ShowSeatAvailability.HELD)
                .map(ShowSeat::getCurrentHoldId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return seatHoldRepository.findAllById(holdIds).stream()
                .collect(Collectors.toMap(SeatHold::getId, Function.identity()));
    }

    private boolean isEffectivelyAvailable(
            ShowSeat seat,
            Map<UUID, SeatHold> currentHolds,
            Instant acquiredAt
    ) {
        if (seat.getAvailabilityStatus() == ShowSeatAvailability.AVAILABLE) {
            return true;
        }
        SeatHold currentHold = currentHolds.get(seat.getCurrentHoldId());
        return currentHold != null && !currentHold.getExpiresAt().isAfter(acquiredAt);
    }

    private List<ShowSeat> orderForResponse(List<ShowSeat> seats) {
        return seats.stream()
                .sorted(Comparator.comparing(ShowSeat::getRowLabel)
                        .thenComparingInt(ShowSeat::getSeatNumber)
                        .thenComparing(ShowSeat::getId))
                .toList();
    }
}
