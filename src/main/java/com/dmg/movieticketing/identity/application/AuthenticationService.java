package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.application.exception.InvalidCredentialsException;
import com.dmg.movieticketing.identity.security.AccountPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Authenticates email/password credentials and issues the configured access-token representation.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            AccessTokenIssuer accessTokenIssuer
    ) {
        this.authenticationManager = authenticationManager;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public SignInResult signIn(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password)
            );
            AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
            IssuedAccessToken token = accessTokenIssuer.issue(principal);
            long expiresIn = Duration.between(token.issuedAt(), token.expiresAt()).toSeconds();

            return new SignInResult(
                    "Bearer",
                    token.value(),
                    expiresIn,
                    principal.accountId(),
                    principal.role()
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException(exception);
        }
    }
}
