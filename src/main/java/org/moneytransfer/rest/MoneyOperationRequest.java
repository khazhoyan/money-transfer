package org.moneytransfer.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public final class MoneyOperationRequest {
    private final BigDecimal amount;

    public MoneyOperationRequest(@JsonProperty(value = "amount", required = true) BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
