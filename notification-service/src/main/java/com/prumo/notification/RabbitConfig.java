package com.prumo.notification;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "prumo.transactions";
    public static final String QUEUE = "prumo.notifications.transactions";

    @Bean
    TopicExchange transactionExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    Queue transactionQueue() { return new Queue(QUEUE, true); }

    @Bean
    Binding createdBinding(Queue transactionQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue).to(transactionExchange).with("TransactionCreated");
    }

    @Bean
    Binding updatedBinding(Queue transactionQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue).to(transactionExchange).with("TransactionUpdated");
    }

    @Bean
    Binding deletedBinding(Queue transactionQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue).to(transactionExchange).with("TransactionDeleted");
    }
}
