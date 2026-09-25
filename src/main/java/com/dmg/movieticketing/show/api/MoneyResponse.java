package com.dmg.movieticketing.show.api;

import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {
}
