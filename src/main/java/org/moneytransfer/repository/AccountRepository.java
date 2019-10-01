package org.moneytransfer.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class AccountRepository {
    private final ConcurrentMap<Long, Account> storage = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public Optional<Account> find(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Creates new account.
     * Note that ids of accounts are consecutive, i.e. when N accounts are created, they will have ids from 0 to N-1.
     *
     * @return newly created account
     */
    public Account create() {
        var id = nextId.getAndIncrement();
        var account = new Account(id, BigDecimal.ZERO);
        storage.put(id, account);
        return account;
    }
}
