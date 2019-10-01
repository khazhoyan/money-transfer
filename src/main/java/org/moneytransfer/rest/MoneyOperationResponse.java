package org.moneytransfer.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.moneytransfer.service.MoneyServiceError;

public class MoneyOperationResponse {
    private final MoneyServiceError error;
    private final String description;

    public static MoneyOperationResponse ok(String description) {
        return new MoneyOperationResponse(null, description);
    }

    public static MoneyOperationResponse error(MoneyServiceError error, String description) {
        return new MoneyOperationResponse(error, description);
    }

    @JsonCreator
    public MoneyOperationResponse(@JsonProperty("error") MoneyServiceError error,
                                  @JsonProperty("description") String description) {
        this.error = error;
        this.description = description;
    }

    public MoneyServiceError getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }
}
