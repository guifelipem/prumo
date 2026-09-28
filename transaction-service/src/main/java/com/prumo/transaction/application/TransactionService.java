package com.prumo.transaction.application;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.persistence.TransactionRepository;
import com.prumo.transaction.persistence.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final AccountClient accounts;
    private final TransactionRepository repository;
    private final CategoryRepository categories;

    public TransactionService(AccountClient accounts, TransactionRepository repository, CategoryRepository categories) {
        this.accounts = accounts;
        this.repository = repository;
        this.categories = categories;
    }

    public Transaction create(String authorization, UUID accountId, TransactionType type,
                              BigDecimal amount, String description, Instant occurredAt, UUID categoryId) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        if (categoryId != null && !categories.supports(ownerId, categoryId, type)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria indisponível para esta transação");
        }
        Transaction transaction = new Transaction(UUID.randomUUID(), accountId, type,
                amount, description.trim(), occurredAt == null ? Instant.now() : occurredAt, categoryId);
        repository.insert(transaction, ownerId);
        return transaction;
    }

    public List<Transaction> list(String authorization, UUID accountId, int page, int size) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        return repository.listForAccount(ownerId, accountId, (long) page * size, size);
    }

    public BigDecimal balance(String authorization, UUID accountId) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        return repository.balanceForAccount(ownerId, accountId);
    }
}
