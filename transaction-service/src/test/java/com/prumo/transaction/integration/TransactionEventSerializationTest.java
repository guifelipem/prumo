package com.prumo.transaction.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.core.MessageProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionEventSerializationTest {
    @Test
    void serializesEventAsJsonForTheConsumer() throws Exception {
        Transaction transaction = new Transaction(UUID.randomUUID(), UUID.randomUUID(),
                TransactionType.EXPENSE, new BigDecimal("12.50"), "Café", Instant.now(), null, Instant.now());
        TransactionEvent event = TransactionEvent.of("TransactionCreated", UUID.randomUUID(), transaction);
        byte[] body = new RabbitConfig().jsonMessageConverter().toMessage(event, new MessageProperties()).getBody();
        JsonNode json = new ObjectMapper().readTree(body);

        assertEquals("TransactionCreated", json.get("eventType").asText());
        assertEquals(transaction.id().toString(), json.get("transactionId").asText());
        assertEquals(transaction.id().toString(), json.get("transaction").get("id").asText());
    }
}
