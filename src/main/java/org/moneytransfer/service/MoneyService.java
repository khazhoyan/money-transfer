package org.moneytransfer.service;


import org.moneytransfer.repository.Account;
import org.moneytransfer.repository.AccountRepository;

import java.math.BigDecimal;

public final class MoneyService {

    private final AccountRepository accountRepository;

    public MoneyService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountDto createAccount() {
        return accountToDto(accountRepository.create());
    }

    public AccountDto getAccount(long id) throws MoneyServiceException {
        var account = accountRepository.find(id).orElseThrow(() -> accountNotFoundException(id));
        return accountToDto(account);
    }

    public void deposit(long id, BigDecimal amount) throws MoneyServiceException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw amountNotPositiveException(amount);
        }
        var account = accountRepository.find(id).orElseThrow(() -> accountNotFoundException(id));
        synchronized (account) {
            doDeposit(account, amount);
        }
    }

    public void withdraw(long id, BigDecimal amount) throws MoneyServiceException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw amountNotPositiveException(amount);
        }
        var account = accountRepository.find(id).orElseThrow(() -> accountNotFoundException(id));
        synchronized (account) {
            doWithdraw(account, amount);
        }
    }

    public void transfer(long idFrom, long idTo, BigDecimal amount) throws MoneyServiceException {
        if (idFrom == idTo) {
            throw sameAccountException(idFrom);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw amountNotPositiveException(amount);
        }

        var from = accountRepository.find(idFrom).orElseThrow(() -> accountNotFoundException(idFrom));
        var to = accountRepository.find(idTo).orElseThrow(() -> accountNotFoundException(idTo));

        // to prevent deadlock
        var first = idFrom < idTo ? from : to;
        var second = idFrom < idTo ? to : from;

        synchronized (first) {
            synchronized (second) {
                doWithdraw(from, amount);
                // at this point, `doDeposit` is guaranteed to succeed, so no data will be corrupted
                doDeposit(to, amount);
            }
        }
    }

    private static AccountDto accountToDto(Account account) {
        return new AccountDto(account.getId(), account.getBalance());
    }

    private static MoneyServiceException sameAccountException(long id) {
        return new MoneyServiceException(
                MoneyServiceError.SAME_ACCOUNT,
                String.format("Attempt to transfer money from account %d to itself", id)
        );
    }

    private static MoneyServiceException amountNotPositiveException(BigDecimal amount) {
        return new MoneyServiceException(
                MoneyServiceError.AMOUNT_NOT_POSITIVE,
                String.format("Expected positive amount, got %s", amount)
        );
    }

    private static MoneyServiceException accountNotFoundException(long id) {
        return new MoneyServiceException(
                MoneyServiceError.ACCOUNT_NOT_FOUND,
                String.format("Could not find account %d", id)
        );
    }

    private static MoneyServiceException insufficientBalanceException(BigDecimal amount, BigDecimal balance) {
        return new MoneyServiceException(
                MoneyServiceError.INSUFFICIENT_BALANCE,
                String.format("%s is too much to transfer, sender got only %s", amount, balance)
        );
    }

    private static void doDeposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

    private static void doWithdraw(Account account, BigDecimal amount) throws MoneyServiceException {
        if (account.getBalance().compareTo(amount) < 0) {
            throw insufficientBalanceException(amount, account.getBalance());
        }
        account.setBalance(account.getBalance().subtract(amount));
    }
}
