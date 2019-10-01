package org.moneytransfer.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.moneytransfer.service.AccountDto;
import org.moneytransfer.service.MoneyServiceError;

public final class AccountInfoResponse extends MoneyOperationResponse {
    private final AccountDto account;

    public static AccountInfoResponse ok(AccountDto account) {
        return new AccountInfoResponse(account, null, null);
    }

    @JsonCreator
    public AccountInfoResponse(@JsonProperty("account") AccountDto account,
                               @JsonProperty("error") MoneyServiceError error,
                               @JsonProperty("description") String description) {
        super(error, description);
        this.account = account;
    }

    public AccountDto getAccount() {
        return account;
    }
}
