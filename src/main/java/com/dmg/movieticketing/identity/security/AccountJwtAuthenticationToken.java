package com.dmg.movieticketing.identity.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class AccountJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final AccountPrincipal principal;
    private final Jwt jwt;

    public AccountJwtAuthenticationToken(
            AccountPrincipal principal,
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public AccountPrincipal getPrincipal() {
        return principal;
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public String getName() {
        return principal.accountId().toString();
    }
}

