package org.moneytransfer.service;

public final class MoneyServiceException extends Exception {
    private final MoneyServiceError errorStatus;

    public MoneyServiceException(MoneyServiceError errorStatus, String message) {
        super(message);
        this.errorStatus = errorStatus;
    }

    public MoneyServiceError getErrorStatus() {
        return errorStatus;
    }
}
