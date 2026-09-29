package com.prumo.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record Transaction(UUID id, UUID accountId, TransactionType type,
                          @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount, String description, Instant occurredAt, UUID categoryId,
                          Instant createdAt, UUID transferId) {
    public Transaction(UUID id, UUID accountId, TransactionType type, BigDecimal amount,
                       String description, Instant occurredAt, UUID categoryId, Instant createdAt) {
        this(id, accountId, type, amount, description, occurredAt, categoryId, createdAt, null);
    }
}
