package com.prumo.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transfer(UUID id, UUID ownerId, UUID sourceAccountId, UUID destinationAccountId,
                       BigDecimal amount, String description, Instant occurredAt, Instant createdAt) {
}
