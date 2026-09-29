package com.prumo.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionEventConsumerTest {
    private final TransactionEventConsumer consumer = new TransactionEventConsumer(new ObjectMapper());

    @Test
    void acceptsValidEvent() {
        String json = """
                {"eventId":"5aa3c8dd-42ad-4eb2-86f9-0ce7644ef922","eventType":"TransactionCreated",
                "occurredAt":"2026-01-01T12:00:00Z","ownerId":"2bd43143-3fb6-4a1d-9066-d85042fc7898",
                "transactionId":"fd4ec6da-161b-4f0e-bd8b-02076470f04b","accountId":"89b298cb-8bac-4fa8-b6cb-4a603db76b9e",
                "transaction":{"id":"fd4ec6da-161b-4f0e-bd8b-02076470f04b",
                "accountId":"89b298cb-8bac-4fa8-b6cb-4a603db76b9e"}}
                """;
        assertDoesNotThrow(() -> consumer.consume(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsInvalidEventWithoutRequeue() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.consume("{}".getBytes(StandardCharsets.UTF_8)));
    }
}
