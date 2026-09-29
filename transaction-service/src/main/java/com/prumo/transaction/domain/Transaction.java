package com.prumo.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(UUID id, UUID accountId, TransactionType type,
                          BigDecimal amount, String description, Instant occurredAt, UUID categoryId,
                          Instant createdAt, UUID transferId) {
    public Transaction(UUID id, UUID accountId, TransactionType type, BigDecimal amount,
                       String description, Instant occurredAt, UUID categoryId, Instant createdAt) {
        this(id, accountId, type, amount, description, occurredAt, categoryId, createdAt, null);
    }
}
