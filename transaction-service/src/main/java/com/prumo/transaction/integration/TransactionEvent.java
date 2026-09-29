package com.prumo.transaction.integration;

import com.prumo.transaction.domain.Transaction;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;

public record TransactionEvent(UUID eventId, String eventType, Instant occurredAt,
                               UUID ownerId, UUID transactionId, UUID accountId, Transaction transaction,
                               String correlationId) {
    public static TransactionEvent of(String eventType, UUID ownerId, Transaction transaction) {
        return new TransactionEvent(UUID.randomUUID(), eventType, Instant.now(), ownerId,
                transaction.id(), transaction.accountId(), transaction, MDC.get("correlationId"));
    }
}
