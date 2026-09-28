package com.prumo.transaction.application;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final AccountClient accounts;
    private final TransactionRepository repository;

    public TransactionService(AccountClient accounts, TransactionRepository repository) {
        this.accounts = accounts;
        this.repository = repository;
    }

    public Transaction create(String authorization, UUID accountId, TransactionType type,
                              BigDecimal amount, String description) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        Transaction transaction = new Transaction(UUID.randomUUID(), accountId, type,
                amount, description.trim(), Instant.now());
        repository.insert(transaction, ownerId);
        return transaction;
    }

    public List<Transaction> list(String authorization, UUID accountId, int page, int size) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        return repository.listForAccount(ownerId, accountId, (long) page * size, size);
    }
}
