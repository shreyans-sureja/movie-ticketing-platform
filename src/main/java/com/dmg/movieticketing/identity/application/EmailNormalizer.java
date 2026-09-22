package com.dmg.movieticketing.identity.application;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

