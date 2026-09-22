package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.domain.AccountRole;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        UserAccountRepository accountRepository = inMemoryRepositoryBoundary();
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded-password";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };

        registrationService = new RegistrationService(
                accountRepository,
                passwordEncoder,
                new EmailNormalizer(),
                new PasswordPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void customerEndpointCreatesCustomerRole() {
        RegistrationResult result = registrationService.registerCustomer(
                "customer@example.com",
                "customer-password"
        );

        assertThat(result.role()).isEqualTo(AccountRole.CUSTOMER);
        assertThat(result.createdAt()).isEqualTo(NOW);
    }

    @Test
    void adminEndpointCreatesTheatreAdminRole() {
        RegistrationResult result = registrationService.registerTheatreAdmin(
                "admin@example.com",
                "administrator-password"
        );

        assertThat(result.role()).isEqualTo(AccountRole.THEATRE_ADMIN);
    }

    private UserAccountRepository inMemoryRepositoryBoundary() {
        return (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "existsByEmailNormalized" -> false;
                    case "saveAndFlush" -> arguments[0];
                    case "toString" -> "RegistrationServiceTestRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
