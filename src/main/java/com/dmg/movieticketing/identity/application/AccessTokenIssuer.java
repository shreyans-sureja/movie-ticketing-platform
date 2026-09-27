package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.security.AccountPrincipal;

/** Application port that keeps sign-in independent of a specific token format. */
public interface AccessTokenIssuer {

    IssuedAccessToken issue(AccountPrincipal principal);
}
