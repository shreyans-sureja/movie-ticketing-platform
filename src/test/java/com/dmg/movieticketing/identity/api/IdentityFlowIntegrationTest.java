package com.dmg.movieticketing.identity.api;

import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IdentityFlowIntegrationTest {

    private static final String JWT_SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserAccountRepository accountRepository;

    @BeforeEach
    void clearAccounts() {
        accountRepository.deleteAll();
    }

    @Test
    void customerCanSignupSignInAndReadCurrentAccount() throws Exception {
        String signupResponse = mockMvc.perform(post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("  Customer@Example.COM  ", "customer-password")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accountId = objectMapper.readTree(signupResponse).get("id").asText();

        String signInResponse = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signInBody("customer@example.com", "customer-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.account.id").value(accountId))
                .andExpect(jsonPath("$.account.role").value("CUSTOMER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(signInResponse).get("accessToken").asText();

        Jwt jwt = jwtDecoder.decode(accessToken);
        assertThat(jwt.getSubject()).isEqualTo(accountId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void theatreAdminSignupIsPublicAndAssignsTheatreAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admins/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("admin@example.com", "administrator-password")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("THEATRE_ADMIN"));
    }

    @Test
    void duplicateEmailAcrossSignupEndpointsReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("same@example.com", "customer-password")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/admins/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("SAME@example.com", "administrator-password")))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void invalidCredentialsUseGenericUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signInBody("unknown@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void currentAccountRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void malformedBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void concurrentSignupAllowsOnlyOneAccountForAnEmail() throws Exception {
        String body = signupBody("race@example.com", "customer-password");
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> signupAfter(start, body));
            Future<Integer> second = executor.submit(() -> signupAfter(start, body));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void weakPasswordReturnsFieldValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("customer@example.com", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("password"));
    }

    private String signupBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new SignupRequest(email, password));
    }

    private String signInBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new SignInRequest(email, password));
    }

    private int signupAfter(CountDownLatch start, String body) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
