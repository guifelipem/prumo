package com.prumo.transaction.application;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.integration.TransactionEvent;
import com.prumo.transaction.persistence.OutboxRepository;
import com.prumo.transaction.persistence.TransactionRepository;
import com.prumo.transaction.persistence.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountClient accounts;
    private final TransactionRepository repository;
    private final CategoryRepository categories;
    private final IdentityClient identities;
    private final OutboxRepository events;

    public TransactionService(AccountClient accounts, TransactionRepository repository,
                              CategoryRepository categories, IdentityClient identities,
                              OutboxRepository events) {
        this.accounts = accounts;
        this.repository = repository;
        this.categories = categories;
        this.identities = identities;
        this.events = events;
    }

    @Transactional
    public Transaction create(String authorization, UUID accountId, TransactionType type,
                              BigDecimal amount, String description, Instant occurredAt, UUID categoryId) {
        UUID ownerId = accounts.requireOwner(accountId, authorization);
        if (categoryId != null && !categories.supports(ownerId, categoryId, type)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria indisponível para esta transação");
        }
        Instant createdAt = Instant.now();
        Transaction transaction = new Transaction(UUID.randomUUID(), accountId, type,
                amount, description.trim(), occurredAt == null ? createdAt : occurredAt, categoryId, createdAt);
        repository.insert(transaction, ownerId);
        events.save(TransactionEvent.of("TransactionCreated", ownerId, transaction));
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

    @Transactional
    public Transaction update(String authorization, UUID id, TransactionType type,
                              BigDecimal amount, String description, Instant occurredAt, UUID categoryId) {
        UUID ownerId = identities.requireUser(authorization);
        Transaction current = repository.findOwned(ownerId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (current.transferId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Use a operação de transferência");
        }
        if (categoryId != null && !categories.supports(ownerId, categoryId, type)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria indisponível para esta transação");
        }
        Transaction updated = new Transaction(id, current.accountId(), type, amount,
                description.trim(), occurredAt, categoryId, current.createdAt());
        if (repository.update(ownerId, updated) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        events.save(TransactionEvent.of("TransactionUpdated", ownerId, updated));
        return updated;
    }

    @Transactional
    public void delete(String authorization, UUID id) {
        UUID ownerId = identities.requireUser(authorization);
        Transaction current = repository.findOwned(ownerId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (current.transferId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Use a operação de transferência");
        }
        if (repository.delete(ownerId, id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        events.save(TransactionEvent.of("TransactionDeleted", ownerId, current));
    }
}
