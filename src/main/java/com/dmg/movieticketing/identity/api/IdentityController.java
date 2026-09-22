package com.dmg.movieticketing.identity.api;

import com.dmg.movieticketing.identity.application.AuthenticationService;
import com.dmg.movieticketing.identity.application.CurrentAccountService;
import com.dmg.movieticketing.identity.application.RegistrationResult;
import com.dmg.movieticketing.identity.application.RegistrationService;
import com.dmg.movieticketing.identity.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentityController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final CurrentAccountService currentAccountService;

    public IdentityController(
            RegistrationService registrationService,
            AuthenticationService authenticationService,
            CurrentAccountService currentAccountService
    ) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.currentAccountService = currentAccountService;
    }

    @PostMapping("/customers/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse signupCustomer(@Valid @RequestBody SignupRequest request) {
        RegistrationResult result = registrationService.registerCustomer(request.email(), request.password());
        return AccountResponse.from(result);
    }

    @PostMapping("/admins/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse signupTheatreAdmin(@Valid @RequestBody SignupRequest request) {
        RegistrationResult result = registrationService.registerTheatreAdmin(request.email(), request.password());
        return AccountResponse.from(result);
    }

    @PostMapping("/signin")
    public SignInResponse signIn(@Valid @RequestBody SignInRequest request) {
        return SignInResponse.from(authenticationService.signIn(request.email(), request.password()));
    }

    @GetMapping("/me")
    public AccountResponse currentAccount(@AuthenticationPrincipal AccountPrincipal principal) {
        return AccountResponse.from(currentAccountService.getById(principal.accountId()));
    }
}

