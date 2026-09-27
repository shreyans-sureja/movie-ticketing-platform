package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.booking.domain.BookingItemRepository;
import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.identity.api.SignInRequest;
import com.dmg.movieticketing.identity.api.SignupRequest;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import com.dmg.movieticketing.hold.domain.SeatHoldItemRepository;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.movie.domain.MovieRepository;
import com.dmg.movieticketing.notification.application.NotificationMessage;
import com.dmg.movieticketing.notification.application.NotificationSender;
import com.dmg.movieticketing.notification.application.NotificationType;
import com.dmg.movieticketing.show.domain.MovieShowRepository;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import com.dmg.movieticketing.show.domain.ShowTierPriceRepository;
import com.dmg.movieticketing.theatre.domain.AuditoriumRepository;
import com.dmg.movieticketing.theatre.domain.PhysicalSeatRepository;
import com.dmg.movieticketing.theatre.domain.TheatreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class MovieAndShowIntegrationTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final String JWT_SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> JWT_SECRET);
        registry.add("booking.hold-duration", () -> "PT5M");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatHoldItemRepository seatHoldItemRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private ShowTierPriceRepository showTierPriceRepository;

    @Autowired
    private MovieShowRepository movieShowRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private PhysicalSeatRepository physicalSeatRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NotificationSender notificationSender;

    @BeforeEach
    void clearOperationalData() {
        jdbcTemplate.update("""
                UPDATE show_seat
                SET availability_status = 'AVAILABLE',
                    current_hold_id = NULL,
                    current_booking_id = NULL
                WHERE current_booking_id IS NOT NULL
                """);
        bookingItemRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        seatHoldItemRepository.deleteAllInBatch();
        showSeatRepository.deleteAllInBatch();
        seatHoldRepository.deleteAllInBatch();
        showTierPriceRepository.deleteAllInBatch();
        movieShowRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
        physicalSeatRepository.deleteAllInBatch();
        auditoriumRepository.deleteAllInBatch();
        theatreRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    void catalogueSchedulingAndPublicDiscoveryWorkEndToEnd() throws Exception {
        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "catalogue-admin@example.com",
                "administrator-password"
        );
        String customerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "catalogue-customer@example.com",
                "customer-password"
        );

        mockMvc.perform(post("/api/v1/movies")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieBody("The Last Signal", 128, "hi")))
                .andExpect(status().isForbidden());

        String movieId = createMovie(adminToken, "  The   Last Signal  ", 128, "HI");

        mockMvc.perform(post("/api/v1/movies")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieBody("the last signal", 128, "hi")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MOVIE_ALREADY_EXISTS"));

        mockMvc.perform(get("/api/v1/movies")
                        .param("q", "signal")
                        .param("languageCode", "HI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(movieId))
                .andExpect(jsonPath("$.items[0].title").value("The Last Signal"))
                .andExpect(jsonPath("$.items[0].languageCode").value("hi"));

        mockMvc.perform(get("/api/v1/movies/{movieId}", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synopsis").doesNotExist())
                .andExpect(jsonPath("$.releaseDate").doesNotExist());

        String theatreId = createTheatre(adminToken, "Central Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");

        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String showId = createShow(adminToken, theatreId, auditoriumId, movieId, startsAt);

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/shows", theatreId, auditoriumId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showBody(movieId, startsAt.plus(30, ChronoUnit.MINUTES))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHOW_TIME_CONFLICT"));

        String localDate = startsAt.atZone(KOLKATA).toLocalDate().toString();
        mockMvc.perform(get("/api/v1/shows")
                        .param("cityId", "7")
                        .param("movieId", movieId)
                        .param("date", localDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(showId))
                .andExpect(jsonPath("$.items[0].minimumPrice").value(250.00))
                .andExpect(jsonPath("$.items[0].maximumPrice").value(400.00))
                .andExpect(jsonPath("$.items[0].availableSeatCount").value(3));

        mockMvc.perform(get("/api/v1/shows/{showId}", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.totalSeatCount").value(3))
                .andExpect(jsonPath("$.tierPrices.length()").value(2));

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].showSeatId").isNotEmpty())
                .andExpect(jsonPath("$.items[0].physicalSeatId").doesNotExist())
                .andExpect(jsonPath("$.items[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$.items[0].price.amount").value(250.00))
                .andExpect(jsonPath("$.items[0].price.currency").value("INR"));

        Integer seatLabelColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'show_seat'
                  AND column_name = 'seat_label'
                """,
                Integer.class
        );
        assertThat(seatLabelColumns).isZero();
    }

    @Test
    void auditoriumRowLockSerializesOnlyThatAuditorium() throws Exception {
        String token = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "concurrency-admin@example.com",
                "administrator-password"
        );
        String movieId = createMovie(token, "Concurrency Film", 120, "en");
        String theatreId = createTheatre(token, "Concurrency Cinema");
        String firstAuditorium = createAuditorium(token, theatreId, "Screen 1");
        String secondAuditorium = createAuditorium(token, theatreId, "Screen 2");
        createSeatRow(token, theatreId, firstAuditorium, "A", 1, 2, "REGULAR");
        createSeatRow(token, theatreId, firstAuditorium, "B", 1, 1, "PREMIUM");
        createSeatRow(token, theatreId, secondAuditorium, "A", 1, 2, "REGULAR");
        createSeatRow(token, theatreId, secondAuditorium, "B", 1, 1, "PREMIUM");
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        List<Integer> sameAuditoriumResults = createShowsConcurrently(
                token,
                theatreId,
                List.of(firstAuditorium, firstAuditorium),
                movieId,
                startsAt
        );
        assertThat(sameAuditoriumResults).containsExactlyInAnyOrder(201, 409);

        Instant otherStart = startsAt.plus(4, ChronoUnit.HOURS);
        List<Integer> differentAuditoriumResults = createShowsConcurrently(
                token,
                theatreId,
                List.of(firstAuditorium, secondAuditorium),
                movieId,
                otherStart
        );
        assertThat(differentAuditoriumResults).containsExactlyInAnyOrder(201, 201);
    }

    @Test
    void malformedPayloadMissingParametersAndInvalidParametersUseStandardProblems() throws Exception {
        String token = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "validation-admin@example.com",
                "administrator-password"
        );

        mockMvc.perform(post("/api/v1/movies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/v1/movies"))
                .andExpect(jsonPath("$.violations").isArray());

        mockMvc.perform(get("/api/v1/shows")
                        .param("movieId", UUID.randomUUID().toString())
                        .param("date", "2026-10-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("cityId"))
                .andExpect(jsonPath("$.violations[0].code").value("CITY_ID_REQUIRED"));

        mockMvc.perform(get("/api/v1/shows")
                        .param("cityId", "7")
                        .param("movieId", UUID.randomUUID().toString())
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("date"))
                .andExpect(jsonPath("$.violations[0].code").value("DATE_TYPE"));
    }

    @Test
    void customerHoldIsOwnerScopedAllOrNothingAndExpiryAware() throws Exception {
        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "hold-admin@example.com",
                "administrator-password"
        );
        String firstCustomerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "hold-customer-one@example.com",
                "customer-password"
        );
        String secondCustomerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "hold-customer-two@example.com",
                "customer-password"
        );
        String movieId = createMovie(adminToken, "Hold Film", 120, "hi");
        String theatreId = createTheatre(adminToken, "Hold Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String showId = createShow(adminToken, theatreId, auditoriumId, movieId, startsAt);
        List<String> seatIds = showSeatIds(showId);

        String holdResponse = mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(firstCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(seatIds.subList(0, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[0].showSeatId").value(seatIds.get(0)))
                .andExpect(jsonPath("$.seats[0].seatLabel").value("A1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String holdId = objectMapper.readTree(holdResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/holds/{holdId}", holdId)
                        .header("Authorization", bearer(firstCustomerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/holds/{holdId}", holdId)
                        .header("Authorization", bearer(secondCustomerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(List.of(seatIds.get(2)))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(secondCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(List.of(seatIds.get(1), seatIds.get(2)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEATS_UNAVAILABLE"));

        assertThat(seatHoldRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availability").value("HELD"))
                .andExpect(jsonPath("$.items[1].availability").value("HELD"))
                .andExpect(jsonPath("$.items[2].availability").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/shows/{showId}", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeatCount").value(1));

        String localDate = startsAt.atZone(KOLKATA).toLocalDate().toString();
        mockMvc.perform(get("/api/v1/shows")
                        .param("cityId", "7")
                        .param("movieId", movieId)
                        .param("date", localDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availableSeatCount").value(1));

        jdbcTemplate.update(
                """
                UPDATE seat_hold
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '10 minutes',
                    expires_at = CURRENT_TIMESTAMP - INTERVAL '5 minutes'
                WHERE id = ?
                """,
                UUID.fromString(holdId)
        );

        mockMvc.perform(get("/api/v1/holds/{holdId}", holdId)
                        .header("Authorization", bearer(firstCustomerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[1].availability").value("AVAILABLE"));

        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(secondCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(List.of(seatIds.get(0)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void overlappingConcurrentHoldRequestsHaveOneAtomicWinner() throws Exception {
        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "hold-race-admin@example.com",
                "administrator-password"
        );
        String firstCustomerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "hold-race-one@example.com",
                "customer-password"
        );
        String secondCustomerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "hold-race-two@example.com",
                "customer-password"
        );
        String movieId = createMovie(adminToken, "Hold Race Film", 120, "en");
        String theatreId = createTheatre(adminToken, "Hold Race Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        String showId = createShow(
                adminToken,
                theatreId,
                auditoriumId,
                movieId,
                Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
        );
        List<String> seatIds = showSeatIds(showId);

        CountDownLatch start = new CountDownLatch(1);
        List<Integer> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> createHoldAfter(
                    start,
                    firstCustomerToken,
                    showId,
                    List.of(seatIds.get(0), seatIds.get(1))
            ));
            Future<Integer> second = executor.submit(() -> createHoldAfter(
                    start,
                    secondCustomerToken,
                    showId,
                    List.of(seatIds.get(2), seatIds.get(1))
            ));
            start.countDown();
            results = List.of(first.get(), second.get());
        }

        assertThat(results).containsExactlyInAnyOrder(201, 409);
        assertThat(seatHoldRepository.count()).isEqualTo(1);
        assertThat(seatHoldItemRepository.count()).isEqualTo(2);
        Integer heldSeats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM show_seat WHERE availability_status = 'HELD'",
                Integer.class
        );
        assertThat(heldSeats).isEqualTo(2);
    }

    @Test
    void customerConfirmsAndCancelsOwnedBookingWithHistoryAndAvailabilityUpdates() throws Exception {
        doThrow(new IllegalStateException("notification provider unavailable"))
                .when(notificationSender)
                .send(any());

        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "booking-admin@example.com",
                "administrator-password"
        );
        String customerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "booking-customer@example.com",
                "customer-password"
        );
        String otherCustomerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "booking-other@example.com",
                "customer-password"
        );
        String movieId = createMovie(adminToken, "Booking Film", 120, "en");
        String theatreId = createTheatre(adminToken, "Booking Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String showId = createShow(adminToken, theatreId, auditoriumId, movieId, startsAt);
        List<String> seatIds = showSeatIds(showId);
        String holdId = createHold(customerToken, showId, seatIds.subList(0, 2));

        mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());

        String bookingBody = mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.holdId").value(holdId))
                .andExpect(jsonPath("$.showId").value(showId))
                .andExpect(jsonPath("$.totalPrice.amount").value(500.00))
                .andExpect(jsonPath("$.totalPrice.currency").value("INR"))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[0].seatLabel").value("A1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bookingId = objectMapper.readTree(bookingBody).get("id").asText();

        mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats.length()").value(2));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(bookingId))
                .andExpect(jsonPath("$.items[0].seatCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/holds/{holdId}", holdId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTED"));

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availability").value("BOOKED"))
                .andExpect(jsonPath("$.items[1].availability").value("BOOKED"))
                .andExpect(jsonPath("$.items[2].availability").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/shows/{showId}", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeatCount").value(1));

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancellation", bookingId)
                        .header("Authorization", bearer(otherCustomerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancellation", bookingId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());

        String cancellationBody = mockMvc.perform(post(
                                "/api/v1/bookings/{bookingId}/cancellation",
                                bookingId
                        )
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isString())
                .andExpect(jsonPath("$.totalPrice.amount").value(500.00))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cancelledAt = objectMapper.readTree(cancellationBody).get("cancelledAt").asText();

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancellation", bookingId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(cancelledAt));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", bookingId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(cancelledAt))
                .andExpect(jsonPath("$.seats.length()").value(2));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.items[0].cancelledAt").value(cancelledAt))
                .andExpect(jsonPath("$.items[0].seatCount").value(2));

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[1].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[2].availability").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/shows/{showId}", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeatCount").value(3));

        mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", bearer(customerToken))
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(bookingItemRepository.count()).isEqualTo(2);
        var notification = org.mockito.ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationSender, times(2)).send(notification.capture());
        assertThat(notification.getAllValues())
                .extracting(NotificationMessage::type)
                .containsExactly(NotificationType.BOOKING_CONFIRMED, NotificationType.BOOKING_CANCELLED);
    }

    @Test
    void concurrentDuplicateConfirmationsCreateOneBookingAndReturnSameResource() throws Exception {
        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "booking-race-admin@example.com",
                "administrator-password"
        );
        String customerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "booking-race-customer@example.com",
                "customer-password"
        );
        String movieId = createMovie(adminToken, "Booking Race Film", 120, "en");
        String theatreId = createTheatre(adminToken, "Booking Race Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        String showId = createShow(
                adminToken,
                theatreId,
                auditoriumId,
                movieId,
                Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
        );
        String holdId = createHold(customerToken, showId, showSeatIds(showId).subList(0, 2));

        CountDownLatch start = new CountDownLatch(1);
        List<ConfirmationHttpResult> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<ConfirmationHttpResult> first = executor.submit(
                    () -> confirmBookingAfter(start, customerToken, holdId)
            );
            Future<ConfirmationHttpResult> second = executor.submit(
                    () -> confirmBookingAfter(start, customerToken, holdId)
            );
            start.countDown();
            results = List.of(first.get(), second.get());
        }

        assertThat(results).extracting(ConfirmationHttpResult::status)
                .containsExactlyInAnyOrder(201, 200);
        assertThat(results).extracting(ConfirmationHttpResult::bookingId).doesNotContainNull();
        assertThat(results.get(0).bookingId()).isEqualTo(results.get(1).bookingId());
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(bookingItemRepository.count()).isEqualTo(2);
        verify(notificationSender, times(1)).send(org.mockito.ArgumentMatchers.argThat(
                message -> message.type() == NotificationType.BOOKING_CONFIRMED
        ));
    }

    @Test
    void concurrentDuplicateCancellationsReleaseSeatsOnceAndReturnSameState() throws Exception {
        String adminToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "cancellation-race-admin@example.com",
                "administrator-password"
        );
        String customerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "cancellation-race-customer@example.com",
                "customer-password"
        );
        String movieId = createMovie(adminToken, "Cancellation Race Film", 120, "en");
        String theatreId = createTheatre(adminToken, "Cancellation Race Cinema");
        String auditoriumId = createAuditorium(adminToken, theatreId, "Screen 1");
        createSeatRow(adminToken, theatreId, auditoriumId, "A", 1, 2, "REGULAR");
        createSeatRow(adminToken, theatreId, auditoriumId, "B", 1, 1, "PREMIUM");
        String showId = createShow(
                adminToken,
                theatreId,
                auditoriumId,
                movieId,
                Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
        );
        String holdId = createHold(customerToken, showId, showSeatIds(showId).subList(0, 2));
        String confirmation = mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bookingId = objectMapper.readTree(confirmation).get("id").asText();

        CountDownLatch start = new CountDownLatch(1);
        List<CancellationHttpResult> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<CancellationHttpResult> first = executor.submit(
                    () -> cancelBookingAfter(start, customerToken, bookingId)
            );
            Future<CancellationHttpResult> second = executor.submit(
                    () -> cancelBookingAfter(start, customerToken, bookingId)
            );
            start.countDown();
            results = List.of(first.get(), second.get());
        }

        assertThat(results).extracting(CancellationHttpResult::status)
                .containsOnly(200);
        assertThat(results).extracting(CancellationHttpResult::bookingId)
                .containsOnly(bookingId);
        assertThat(results).extracting(CancellationHttpResult::cancelledAt)
                .doesNotContainNull()
                .containsOnly(results.getFirst().cancelledAt());
        assertThat(bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow().getStatus().name())
                .isEqualTo("CANCELLED");
        Integer availableSeats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM show_seat WHERE show_id = ? AND availability_status = 'AVAILABLE'",
                Integer.class,
                UUID.fromString(showId)
        );
        assertThat(availableSeats).isEqualTo(3);
        assertThat(bookingItemRepository.count()).isEqualTo(2);
        verify(notificationSender, times(1)).send(org.mockito.ArgumentMatchers.argThat(
                message -> message.type() == NotificationType.BOOKING_CONFIRMED
        ));
        verify(notificationSender, times(1)).send(org.mockito.ArgumentMatchers.argThat(
                message -> message.type() == NotificationType.BOOKING_CANCELLED
        ));
    }

    private List<Integer> createShowsConcurrently(
            String token,
            String theatreId,
            List<String> auditoriumIds,
            String movieId,
            Instant startsAt
    ) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> createShowAfter(
                    start,
                    token,
                    theatreId,
                    auditoriumIds.get(0),
                    movieId,
                    startsAt
            ));
            Future<Integer> second = executor.submit(() -> createShowAfter(
                    start,
                    token,
                    theatreId,
                    auditoriumIds.get(1),
                    movieId,
                    startsAt
            ));
            start.countDown();
            return List.of(first.get(), second.get());
        }
    }

    private int createShowAfter(
            CountDownLatch start,
            String token,
            String theatreId,
            String auditoriumId,
            String movieId,
            Instant startsAt
    ) throws Exception {
        start.await();
        return mockMvc.perform(post(
                                "/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/shows",
                                theatreId,
                                auditoriumId
                        )
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showBody(movieId, startsAt)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int createHoldAfter(
            CountDownLatch start,
            String token,
            String showId,
            List<String> showSeatIds
    ) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(showSeatIds)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private ConfirmationHttpResult confirmBookingAfter(
            CountDownLatch start,
            String token,
            String holdId
    ) throws Exception {
        start.await();
        var response = mockMvc.perform(post("/api/v1/holds/{holdId}/booking", holdId)
                        .header("Authorization", bearer(token)))
                .andReturn()
                .getResponse();
        String bookingId = objectMapper.readTree(response.getContentAsString()).path("id").asText(null);
        return new ConfirmationHttpResult(response.getStatus(), bookingId);
    }

    private CancellationHttpResult cancelBookingAfter(
            CountDownLatch start,
            String token,
            String bookingId
    ) throws Exception {
        start.await();
        var response = mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancellation", bookingId)
                        .header("Authorization", bearer(token)))
                .andReturn()
                .getResponse();
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        return new CancellationHttpResult(
                response.getStatus(),
                body.path("id").asText(null),
                body.path("cancelledAt").asText(null)
        );
    }

    private List<String> showSeatIds(String showId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = new java.util.ArrayList<>();
        objectMapper.readTree(response).get("items").forEach(item -> ids.add(item.get("showSeatId").asText()));
        return List.copyOf(ids);
    }

    private String holdBody(List<String> showSeatIds) throws Exception {
        return objectMapper.writeValueAsString(Map.of("showSeatIds", showSeatIds));
    }

    private String createHold(String token, String showId, List<String> showSeatIds) throws Exception {
        String response = mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(showSeatIds)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String signupAndSignIn(String signupPath, String email, String password) throws Exception {
        mockMvc.perform(post(signupPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password))))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignInRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createMovie(String token, String title, int duration, String language) throws Exception {
        String response = mockMvc.perform(post("/api/v1/movies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieBody(title, duration, language)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createTheatre(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/theatres")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cityId", 7,
                                "name", name,
                                "addressLine1", "14 River Road",
                                "postalCode", "380009"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createAuditorium(String token, String theatreId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/theatres/{theatreId}/auditoriums", theatreId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void createSeatRow(
            String token,
            String theatreId,
            String auditoriumId,
            String rowLabel,
            int firstSeat,
            int seatCount,
            String tier
    ) throws Exception {
        mockMvc.perform(post(
                                "/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seat-rows",
                                theatreId,
                                auditoriumId
                        )
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "rowLabel", rowLabel,
                                "firstSeatNumber", firstSeat,
                                "seatCount", seatCount,
                                "tier", tier
                        ))))
                .andExpect(status().isCreated());
    }

    private String createShow(
            String token,
            String theatreId,
            String auditoriumId,
            String movieId,
            Instant startsAt
    ) throws Exception {
        String response = mockMvc.perform(post(
                                "/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/shows",
                                theatreId,
                                auditoriumId
                        )
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showBody(movieId, startsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tierPrices[0].amount").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String movieBody(String title, int duration, String language) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "durationMinutes", duration,
                "languageCode", language
        ));
    }

    private String showBody(String movieId, Instant startsAt) throws Exception {
        JsonNode body = objectMapper.valueToTree(Map.of(
                "movieId", movieId,
                "startsAt", startsAt.toString(),
                "currency", "INR",
                "tierPrices", List.of(
                        Map.of("tier", "REGULAR", "amount", new BigDecimal("250.00")),
                        Map.of("tier", "PREMIUM", "amount", new BigDecimal("400.00"))
                )
        ));
        return objectMapper.writeValueAsString(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record ConfirmationHttpResult(int status, String bookingId) {
    }

    private record CancellationHttpResult(int status, String bookingId, String cancelledAt) {
    }
}
