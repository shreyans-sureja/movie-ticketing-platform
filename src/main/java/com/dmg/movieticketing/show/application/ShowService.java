package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.city.application.CityNotFoundException;
import com.dmg.movieticketing.city.domain.CityRepository;
import com.dmg.movieticketing.movie.application.MovieNotFoundException;
import com.dmg.movieticketing.movie.domain.Movie;
import com.dmg.movieticketing.movie.domain.MovieRepository;
import com.dmg.movieticketing.show.domain.MovieShow;
import com.dmg.movieticketing.show.domain.MovieShowRepository;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import com.dmg.movieticketing.show.domain.ShowTierPrice;
import com.dmg.movieticketing.show.domain.ShowTierPriceRepository;
import com.dmg.movieticketing.theatre.application.AuditoriumNotFoundException;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import com.dmg.movieticketing.theatre.application.TheatreNotFoundException;
import com.dmg.movieticketing.theatre.domain.Auditorium;
import com.dmg.movieticketing.theatre.domain.AuditoriumRepository;
import com.dmg.movieticketing.theatre.domain.PhysicalSeat;
import com.dmg.movieticketing.theatre.domain.PhysicalSeatRepository;
import com.dmg.movieticketing.theatre.domain.SeatTier;
import com.dmg.movieticketing.theatre.domain.TheatreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Schedules shows, snapshots their sellable seats, and serves public discovery data.
 */
@Service
public class ShowService {

    private static final ZoneId CUSTOMER_ZONE = ZoneId.of("Asia/Kolkata");

    private final CityRepository cityRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final PhysicalSeatRepository physicalSeatRepository;
    private final MovieShowRepository movieShowRepository;
    private final ShowTierPriceRepository showTierPriceRepository;
    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    public ShowService(
            CityRepository cityRepository,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            AuditoriumRepository auditoriumRepository,
            PhysicalSeatRepository physicalSeatRepository,
            MovieShowRepository movieShowRepository,
            ShowTierPriceRepository showTierPriceRepository,
            ShowSeatRepository showSeatRepository,
            Clock clock
    ) {
        this.cityRepository = cityRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.physicalSeatRepository = physicalSeatRepository;
        this.movieShowRepository = movieShowRepository;
        this.showTierPriceRepository = showTierPriceRepository;
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
    }

    /**
     * Creates one future show in an owned auditorium with complete pricing for
     * every physical-seat tier present in that auditorium.
     */
    @Transactional
    public ShowDetails createShow(
            UUID accountId,
            UUID theatreId,
            UUID auditoriumId,
            UUID movieId,
            Instant startsAt,
            String currency,
            List<TierPriceInput> tierPriceInputs
    ) {
        theatreRepository.findByIdAndOwnerAccountId(theatreId, accountId)
                .orElseThrow(TheatreNotFoundException::new);
        // Locking the auditorium serializes overlap checks only for this auditorium.
        Auditorium auditorium = auditoriumRepository.findByIdAndTheatreIdForUpdate(auditoriumId, theatreId)
                .orElseThrow(AuditoriumNotFoundException::new);
        Movie movie = movieRepository.findById(movieId).orElseThrow(MovieNotFoundException::new);

        Instant requestNow = clock.instant();
        if (!startsAt.isAfter(requestNow)) {
            throw new DomainValidationException(
                    "startsAt",
                    "SHOW_START_FUTURE",
                    "Show start must be in the future."
            );
        }

        List<PhysicalSeat> physicalSeats = physicalSeatRepository
                .findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAscIdAsc(auditoriumId);
        if (physicalSeats.isEmpty()) {
            throw new AuditoriumHasNoSeatsException();
        }

        String normalizedCurrency = validateCurrency(currency);
        Map<SeatTier, BigDecimal> prices = validateTierPrices(physicalSeats, tierPriceInputs);
        Instant endsAt = startsAt.plus(movie.getDurationMinutes(), ChronoUnit.MINUTES);

        // This check is safe from concurrent inserts because the auditorium row is still locked.
        if (movieShowRepository.existsByAuditoriumIdAndStartsAtLessThanAndEndsAtGreaterThan(
                auditoriumId,
                endsAt,
                startsAt
        )) {
            throw new ShowTimeConflictException();
        }

        MovieShow show = MovieShow.create(
                UUID.randomUUID(),
                movie,
                auditorium,
                accountId,
                startsAt,
                endsAt,
                normalizedCurrency,
                requestNow
        );
        movieShowRepository.save(show);

        List<ShowTierPrice> tierPrices = prices.entrySet().stream()
                .map(entry -> ShowTierPrice.create(
                        show,
                        entry.getKey(),
                        entry.getValue(),
                        normalizedCurrency
                ))
                .toList();
        showTierPriceRepository.saveAll(tierPrices);

        // Snapshot layout and final price so later physical-seat changes cannot alter this show.
        List<ShowSeat> showSeats = new ArrayList<>(physicalSeats.size());
        for (PhysicalSeat physicalSeat : physicalSeats) {
            showSeats.add(ShowSeat.create(
                    UUID.randomUUID(),
                    show,
                    physicalSeat,
                    prices.get(physicalSeat.getTier()),
                    normalizedCurrency,
                    requestNow
            ));
        }
        showSeatRepository.saveAllAndFlush(showSeats);

        return new ShowDetails(show, List.copyOf(tierPrices), showSeats.size(), showSeats.size());
    }

    /**
     * Searches upcoming shows for a customer date interpreted in Asia/Kolkata.
     */
    @Transactional(readOnly = true)
    public Page<ShowSearchItem> searchShows(
            long cityId,
            UUID movieId,
            LocalDate date,
            Pageable pageable
    ) {
        if (!cityRepository.existsById(cityId)) {
            throw new CityNotFoundException();
        }
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException();
        }

        Instant requestNow = clock.instant();
        LocalDate currentCustomerDate = requestNow.atZone(CUSTOMER_ZONE).toLocalDate();
        if (date.isBefore(currentCustomerDate)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Convert only the search boundary to UTC; persisted timestamps remain UTC instants.
        Instant dayStart = date.atStartOfDay(CUSTOMER_ZONE).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(CUSTOMER_ZONE).toInstant();
        return movieShowRepository.searchUpcoming(
                cityId,
                movieId,
                dayStart,
                dayEnd,
                requestNow,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public ShowDetails getShow(UUID showId) {
        MovieShow show = findShow(showId);
        Instant requestNow = clock.instant();
        return new ShowDetails(
                show,
                List.copyOf(showTierPriceRepository.findAllByShowIdOrderByTierAsc(showId)),
                showSeatRepository.countByShowId(showId),
                showSeatRepository.countEffectivelyAvailable(showId, requestNow)
        );
    }

    @Transactional(readOnly = true)
    public ShowSeatListing listShowSeats(UUID showId) {
        MovieShow show = findShow(showId);
        Instant requestNow = clock.instant();
        return new ShowSeatListing(
                showId,
                show.getCurrency(),
                showSeatRepository.findAllWithCurrentHoldExpiry(showId).stream()
                        .map(data -> new ShowSeatView(data.seat(), data.effectiveAvailabilityAt(requestNow)))
                        .toList()
        );
    }

    private MovieShow findShow(UUID showId) {
        return movieShowRepository.findById(showId).orElseThrow(ShowNotFoundException::new);
    }

    private String validateCurrency(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw new DomainValidationException(
                    "currency",
                    "CURRENCY_INVALID",
                    "Currency must be a supported ISO 4217 code."
            );
        }
        return normalized;
    }

    private Map<SeatTier, BigDecimal> validateTierPrices(
            List<PhysicalSeat> physicalSeats,
            List<TierPriceInput> inputs
    ) {
        Set<SeatTier> requiredTiers = EnumSet.noneOf(SeatTier.class);
        physicalSeats.forEach(seat -> requiredTiers.add(seat.getTier()));

        Map<SeatTier, BigDecimal> prices = new EnumMap<>(SeatTier.class);
        for (TierPriceInput input : inputs) {
            if (input == null || input.tier() == null || input.amount() == null) {
                throw new TierPriceMismatchException();
            }
            BigDecimal amount = validateAmount(input.amount());
            if (prices.putIfAbsent(input.tier(), amount) != null) {
                throw new TierPriceMismatchException();
            }
        }

        // Exact equality prevents creating show seats with a missing or irrelevant tier price.
        if (!prices.keySet().equals(requiredTiers)) {
            throw new TierPriceMismatchException();
        }
        return prices;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount.signum() <= 0 || amount.scale() > 2) {
            throw invalidPrice();
        }
        BigDecimal scaled;
        try {
            scaled = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalidPrice();
        }
        if (scaled.precision() > 12) {
            throw invalidPrice();
        }
        return scaled;
    }

    private DomainValidationException invalidPrice() {
        return new DomainValidationException(
                "tierPrices",
                "PRICE_INVALID",
                "Every price must be positive, have at most two fractional digits, and fit NUMERIC(12,2)."
        );
    }
}
