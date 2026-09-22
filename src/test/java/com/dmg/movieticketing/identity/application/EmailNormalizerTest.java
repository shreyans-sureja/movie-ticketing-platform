package com.dmg.movieticketing.identity.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizerTest {

    private final EmailNormalizer normalizer = new EmailNormalizer();

    @Test
    void trimsAndLowercasesUsingStableRules() {
        assertThat(normalizer.normalize("  Customer.Example@EXAMPLE.COM  "))
                .isEqualTo("customer.example@example.com");
    }

    @Test
    void doesNotApplyProviderSpecificTransformations() {
        assertThat(normalizer.normalize("first.last+movies@example.com"))
                .isEqualTo("first.last+movies@example.com");
    }
}

