package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.application.exception.EmailAlreadyRegisteredException;
import com.dmg.movieticketing.identity.domain.AccountRole;
import com.dmg.movieticketing.identity.domain.UserAccount;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RegistrationService {

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public RegistrationService(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    @Transactional
    public RegistrationResult registerCustomer(String email, String password) {
        return register(email, password, AccountRole.CUSTOMER);
    }

    @Transactional
    public RegistrationResult registerTheatreAdmin(String email, String password) {
        return register(email, password, AccountRole.THEATRE_ADMIN);
    }

    private RegistrationResult register(String email, String password, AccountRole role) {
        passwordPolicy.validateForBcrypt(password);
        String normalizedEmail = emailNormalizer.normalize(email);

        if (accountRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant now = clock.instant();
        UserAccount account = UserAccount.create(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(password),
                role,
                now
        );

        try {
            UserAccount saved = accountRepository.saveAndFlush(account);
            return toResult(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException(exception);
        }
    }

    private RegistrationResult toResult(UserAccount account) {
        return new RegistrationResult(
                account.getId(),
                account.getEmailNormalized(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}

