package com.prumo.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private static final Set<String> TYPES = Set.of("TransactionCreated", "TransactionUpdated", "TransactionDeleted");
    private final ObjectMapper mapper;

    public TransactionEventConsumer(ObjectMapper mapper) { this.mapper = mapper; }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(byte[] body) {
        try {
            JsonNode event = mapper.readTree(body);
            String correlationId = event.path("correlationId").asText(null);
            if (correlationId != null && !correlationId.isBlank()) MDC.put("correlationId", correlationId);
            UUID eventId = UUID.fromString(required(event, "eventId"));
            String type = required(event, "eventType");
            Instant occurredAt = Instant.parse(required(event, "occurredAt"));
            UUID ownerId = UUID.fromString(required(event, "ownerId"));
            UUID transactionId = UUID.fromString(required(event, "transactionId"));
            UUID accountId = UUID.fromString(required(event, "accountId"));
            if (!TYPES.contains(type)) throw new IllegalArgumentException("Tipo de evento inválido: " + type);
            JsonNode transaction = event.path("transaction");
            if (!transaction.isObject() || !transactionId.toString().equals(required(transaction, "id"))
                    || !accountId.toString().equals(required(transaction, "accountId"))) {
                throw new IllegalArgumentException("Dados da transação inválidos");
            }
            log.info("Evento consumido: type={} eventId={} transactionId={} accountId={} ownerId={} occurredAt={}",
                    type, eventId, transactionId, accountId, ownerId, occurredAt);
        } catch (IOException | IllegalArgumentException error) {
            log.warn("Evento de transação rejeitado: {}", error.getMessage());
            throw new AmqpRejectAndDontRequeueException("Evento de transação inválido", error);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("Campo ausente: " + field);
        return value.asText();
    }
}
