package com.dmg.movieticketing.identity.security;

import com.dmg.movieticketing.identity.domain.AccountRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Converts verified JWT claims into the application's account principal and role authority.
 */
@Component
public class JwtToAccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID accountId = UUID.fromString(jwt.getSubject());
        AccountRole role = AccountRole.valueOf(jwt.getClaimAsString("role"));
        String authenticationMethod = readAuthenticationMethod(jwt);
        AccountPrincipal principal = new AccountPrincipal(accountId, role, authenticationMethod);

        return new AccountJwtAuthenticationToken(
                principal,
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private String readAuthenticationMethod(Jwt jwt) {
        List<String> methods = jwt.getClaimAsStringList("amr");
        return methods == null || methods.isEmpty() ? "unknown" : methods.getFirst();
    }
}
