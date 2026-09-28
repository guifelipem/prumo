package com.prumo.account.application;

import com.prumo.account.domain.Account;
import com.prumo.account.persistence.AccountRepository;
import java.util.Currency;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public Account create(UUID ownerId, String requestedName, String requestedCurrency) {
        String name = requestedName.trim();
        String code = requestedCurrency.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 3) {
            throw new IllegalArgumentException("Moeda inválida");
        }
        Currency.getInstance(code);
        Account account = new Account(UUID.randomUUID(), ownerId, name, code);
        repository.insert(account);
        return account;
    }

    public Optional<Account> findOwned(UUID id, UUID ownerId) {
        return repository.findOwned(id, ownerId);
    }

    public List<Account> listOwned(UUID ownerId, int page, int size) {
        return repository.listOwned(ownerId, (long) page * size, size);
    }
}
