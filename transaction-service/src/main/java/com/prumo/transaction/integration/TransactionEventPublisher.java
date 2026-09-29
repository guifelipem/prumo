package com.prumo.transaction.integration;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {
    public static final String EXCHANGE = "prumo.transactions";
    private final RabbitTemplate rabbitTemplate;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(TransactionEvent event) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(EXCHANGE, event.eventType(), event);
            operations.waitForConfirmsOrDie(10_000);
            return null;
        });
    }
}
