package org.moneytransfer.service;

import org.junit.Before;
import org.junit.Test;
import org.moneytransfer.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public final class MoneyServiceConcurrencyTest {

    private MoneyService moneyService;

    @Before
    public void setUp() {
        var accountRepository = new AccountRepository();
        moneyService = new MoneyService(accountRepository);
    }

    @Test
    public void worksSafelyWhenCreatingAccountsConcurrently() throws Exception {
        var n = 100;
        var tasks = new ArrayList<MoneyServiceFunction>(n);
        for (int i = 0; i < n; i++) {
            tasks.add(moneyService::createAccount);
        }
        runInParallelAndAwait(tasks);
        // Check that exactly `n` accounts were created
        for (int i = 0; i < n; i++) {
            var account = moneyService.getAccount(i);
            assertEquals(BigDecimal.ZERO, account.getBalance());
        }
        try {
            moneyService.getAccount(n);
            fail("Only `n` accounts must be created. Therefore, there should be no account with `id=n`");
        } catch (MoneyServiceException e) {
            assertEquals(MoneyServiceError.ACCOUNT_NOT_FOUND, e.getErrorStatus());
        }
    }

    @Test
    public void worksSafelyWhenDepositingAndWithdrawingConcurrently() throws Exception {
        var accountId = moneyService.createAccount().getId();
        var n = 100;
        moneyService.deposit(accountId, BigDecimal.valueOf(n));
        var tasks = new ArrayList<MoneyServiceFunction>(n);
        for (int i = 0; i < n; i++) {
            tasks.add(() -> moneyService.deposit(accountId, BigDecimal.ONE));
            tasks.add(() -> moneyService.withdraw(accountId, BigDecimal.ONE));
        }
        runInParallelAndAwait(tasks);
        assertEquals(BigDecimal.valueOf(n), moneyService.getAccount(accountId).getBalance());
    }

    @Test
    public void worksSafelyWhenTransferringConcurrently() throws Exception {
        var fromId = moneyService.createAccount().getId();
        var toId = moneyService.createAccount().getId();
        var n = 100;
        moneyService.deposit(fromId, BigDecimal.valueOf(n));
        var tasks = new ArrayList<MoneyServiceFunction>(n);
        for (int i = 0; i < n; i++) {
            tasks.add(() -> moneyService.transfer(fromId, toId, BigDecimal.ONE));
        }
        runInParallelAndAwait(tasks);
        assertEquals(BigDecimal.ZERO, moneyService.getAccount(fromId).getBalance());
        assertEquals(BigDecimal.valueOf(n), moneyService.getAccount(toId).getBalance());
    }

    private interface MoneyServiceFunction {
        void run() throws MoneyServiceException;
    }

    private static void runInParallelAndAwait(Collection<MoneyServiceFunction> tasks) throws Exception {
        var executorService = Executors.newFixedThreadPool(3);
        var callables = new ArrayList<Callable<Void>>(tasks.size());
        for (MoneyServiceFunction task : tasks) {
            callables.add(() -> {
                task.run();
                return null;
            });
        }
        Collection<Future<Void>> futures = executorService.invokeAll(callables);
        for (Future<Void> future : futures) {
            future.get(1L, TimeUnit.MINUTES);
        }
        executorService.shutdown();
    }
}
