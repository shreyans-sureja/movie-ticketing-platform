package com.dmg.movieticketing.identity.api;

import com.dmg.movieticketing.identity.application.SignInResult;

public record SignInResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        TokenAccountResponse account
) {
    public static SignInResponse from(SignInResult result) {
        return new SignInResponse(
                result.tokenType(),
                result.accessToken(),
                result.expiresIn(),
                new TokenAccountResponse(result.accountId(), result.role())
        );
    }
}

