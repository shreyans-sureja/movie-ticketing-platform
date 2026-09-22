package com.dmg.movieticketing.identity.security;

import com.dmg.movieticketing.identity.application.EmailNormalizer;
import com.dmg.movieticketing.identity.domain.AccountStatus;
import com.dmg.movieticketing.identity.domain.UserAccount;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private static final String DUMMY_PASSWORD = "dummy-password-used-only-for-timing";

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final String dummyPasswordHash;

    public PasswordAuthenticationProvider(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = emailNormalizer.normalize(authentication.getName());
        String password = String.valueOf(authentication.getCredentials());
        Optional<UserAccount> candidate = accountRepository.findByEmailNormalized(email);

        if (candidate.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new BadCredentialsException("Invalid credentials");
        }

        UserAccount account = candidate.get();
        boolean passwordMatches = passwordEncoder.matches(password, account.getPasswordHash());
        if (!passwordMatches || account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Invalid credentials");
        }

        AccountPrincipal principal = new AccountPrincipal(account.getId(), account.getRole(), "password");
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

