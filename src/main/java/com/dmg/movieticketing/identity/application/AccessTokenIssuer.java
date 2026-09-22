package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.security.AccountPrincipal;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(AccountPrincipal principal);
}

