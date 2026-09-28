package com.prumo.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(UUID id, UUID accountId, TransactionType type,
                          BigDecimal amount, String description, Instant occurredAt, UUID categoryId) {
}
