package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.application.exception.AccountNotFoundException;
import com.dmg.movieticketing.identity.domain.UserAccount;
import com.dmg.movieticketing.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CurrentAccountService {

    private final UserAccountRepository accountRepository;

    public CurrentAccountService(UserAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public RegistrationResult getById(UUID accountId) {
        UserAccount account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);

        return new RegistrationResult(
                account.getId(),
                account.getEmailNormalized(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}

