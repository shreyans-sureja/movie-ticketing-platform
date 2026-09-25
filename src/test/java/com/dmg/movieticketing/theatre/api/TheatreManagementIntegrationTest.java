package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.identity.api.SignInRequest;
import com.dmg.movieticketing.identity.api.SignupRequest;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import com.dmg.movieticketing.theatre.domain.AuditoriumRepository;
import com.dmg.movieticketing.theatre.domain.PhysicalSeatRepository;
import com.dmg.movieticketing.theatre.domain.TheatreRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TheatreManagementIntegrationTest {

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
    private PhysicalSeatRepository physicalSeatRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOperationalData() {
        physicalSeatRepository.deleteAllInBatch();
        auditoriumRepository.deleteAllInBatch();
        theatreRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    void cityCatalogueIsPublicAndMinimal() throws Exception {
        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(50))
                .andExpect(jsonPath("$.items[0].name").value("Agra"));

        mockMvc.perform(get("/api/v1/cities/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Ahmedabad"))
                .andExpect(jsonPath("$.stateOrUt").value("Gujarat"));

        Integer extraColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'city'
                  AND column_name NOT IN ('id', 'name', 'state_or_ut')
                """,
                Integer.class
        );
        assertThat(extraColumns).isZero();
    }

    @Test
    void customerCanReadCitiesButCannotManageTheatres() throws Exception {
        String customerToken = signupAndSignIn(
                "/api/v1/auth/customers/signup",
                "customer@example.com",
                "customer-password"
        );

        mockMvc.perform(get("/api/v1/cities")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/theatres")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatreBody("Customer Cinema")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanCreateAndListOwnedTheatreHierarchy() throws Exception {
        String token = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "admin@example.com",
                "administrator-password"
        );
        String theatreId = createTheatre(token, "Central Cinema");

        mockMvc.perform(get("/api/v1/theatres")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(theatreId))
                .andExpect(jsonPath("$.items[0].city.id").value(7));

        String auditoriumResponse = mockMvc.perform(post("/api/v1/theatres/{theatreId}/auditoriums", theatreId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Screen 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String auditoriumId = objectMapper.readTree(auditoriumResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seat-rows", theatreId, auditoriumId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatRowBody("a", 3, 3, "PREMIUM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rowLabel").value("A"))
                .andExpect(jsonPath("$.seats.length()").value(3))
                .andExpect(jsonPath("$.seats[0].seatLabel").value("A3"))
                .andExpect(jsonPath("$.seats[2].seatLabel").value("A5"))
                .andExpect(jsonPath("$.seats[0].tier").value("PREMIUM"));

        mockMvc.perform(get("/api/v1/theatres/{theatreId}/auditoriums", theatreId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].seatCount").value(3));

        mockMvc.perform(get("/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seats", theatreId, auditoriumId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].seatNumber").value(3))
                .andExpect(jsonPath("$.items[0].seatLabel").value("A3"));

        Integer persistedSeatLabelColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'physical_seat'
                  AND column_name = 'seat_label'
                """,
                Integer.class
        );
        assertThat(persistedSeatLabelColumns).isZero();
    }

    @Test
    void ownerIsolationReturnsNotFoundForAnotherAdmin() throws Exception {
        String ownerToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "owner@example.com",
                "administrator-password"
        );
        String otherToken = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "other@example.com",
                "administrator-password"
        );
        String theatreId = createTheatre(ownerToken, "Owner Cinema");

        mockMvc.perform(get("/api/v1/theatres")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/v1/theatres/{theatreId}/auditoriums", theatreId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("THEATRE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/auditoriums", theatreId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Unauthorized Screen"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("THEATRE_NOT_FOUND"));
    }

    @Test
    void overlappingSeatRowsConflictAndRollBackTheWholeRequest() throws Exception {
        String token = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "row-admin@example.com",
                "administrator-password"
        );
        String theatreId = createTheatre(token, "Row Cinema");
        String auditoriumId = createAuditorium(token, theatreId, "Screen 1");

        String path = "/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seat-rows";
        mockMvc.perform(post(path, theatreId, auditoriumId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatRowBody("A", 1, 3, "REGULAR")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(path, theatreId, auditoriumId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatRowBody("A", 3, 3, "PREMIUM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_CONFLICT"));

        assertThat(physicalSeatRepository.count()).isEqualTo(3);
    }

    @Test
    void concurrentOverlappingSeatRowsCreateOnlyOneRange() throws Exception {
        String token = signupAndSignIn(
                "/api/v1/auth/admins/signup",
                "race-admin@example.com",
                "administrator-password"
        );
        String theatreId = createTheatre(token, "Race Cinema");
        String auditoriumId = createAuditorium(token, theatreId, "Screen 1");
        String body = seatRowBody("B", 1, 10, "REGULAR");
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(
                    () -> createSeatRowAfter(start, token, theatreId, auditoriumId, body)
            );
            Future<Integer> second = executor.submit(
                    () -> createSeatRowAfter(start, token, theatreId, auditoriumId, body)
            );
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(physicalSeatRepository.count()).isEqualTo(10);
    }

    private String signupAndSignIn(String signupPath, String email, String password) throws Exception {
        mockMvc.perform(post(signupPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password))))
                .andExpect(status().isCreated());

        String signInResponse = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignInRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(signInResponse).get("accessToken").asText();
    }

    private String createTheatre(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/theatres")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(theatreBody(name)))
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

    private String theatreBody(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "cityId", 7,
                "name", name,
                "addressLine1", "14 River Road",
                "addressLine2", "Navrangpura",
                "postalCode", "380009"
        ));
    }

    private String seatRowBody(String row, int firstSeat, int seatCount, String tier) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "rowLabel", row,
                "firstSeatNumber", firstSeat,
                "seatCount", seatCount,
                "tier", tier
        ));
    }

    private int createSeatRowAfter(
            CountDownLatch start,
            String token,
            String theatreId,
            String auditoriumId,
            String body
    ) throws Exception {
        start.await();
        return mockMvc.perform(post(
                                "/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seat-rows",
                                theatreId,
                                auditoriumId
                        )
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
