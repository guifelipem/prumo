package com.prumo.transaction.integration;

import com.prumo.transaction.domain.Transaction;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(UUID eventId, String eventType, Instant occurredAt,
                               UUID ownerId, UUID transactionId, UUID accountId, Transaction transaction) {
    public static TransactionEvent of(String eventType, UUID ownerId, Transaction transaction) {
        return new TransactionEvent(UUID.randomUUID(), eventType, Instant.now(), ownerId,
                transaction.id(), transaction.accountId(), transaction);
    }
}
