package com.dmg.movieticketing.identity.security;

import com.dmg.movieticketing.identity.application.IssuedAccessToken;
import com.dmg.movieticketing.identity.config.JwtProperties;
import com.dmg.movieticketing.identity.domain.AccountRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes();

    @Test
    void tokenContainsAccountIdAndRole() {
        SecretKey secretKey = new SecretKeySpec(SECRET, "HmacSHA256");
        JwtProperties properties = new JwtProperties(
                "unused-in-this-unit-test",
                "movie-ticketing-platform",
                "movie-ticketing-api",
                Duration.ofMinutes(15),
                Duration.ofSeconds(30)
        );
        JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey)),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        UUID accountId = UUID.randomUUID();

        IssuedAccessToken issued = issuer.issue(
                new AccountPrincipal(accountId, AccountRole.THEATRE_ADMIN, "password")
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt decoded = decoder.decode(issued.value());

        assertThat(decoded.getSubject()).isEqualTo(accountId.toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("THEATRE_ADMIN");
        assertThat(decoded.getClaimAsStringList("amr")).containsExactly("password");
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }
}

