package com.prumo.transaction.integration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange transactionExchange() {
        return new TopicExchange(TransactionEventPublisher.EXCHANGE, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return new Queue("prumo.notifications.transactions", true);
    }

    @Bean
    Binding createdBinding(Queue notificationQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(notificationQueue).to(transactionExchange).with("TransactionCreated");
    }

    @Bean
    Binding updatedBinding(Queue notificationQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(notificationQueue).to(transactionExchange).with("TransactionUpdated");
    }

    @Bean
    Binding deletedBinding(Queue notificationQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(notificationQueue).to(transactionExchange).with("TransactionDeleted");
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
