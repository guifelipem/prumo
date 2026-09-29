package com.prumo.transaction.integration;

import com.prumo.transaction.persistence.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private final OutboxRepository outbox;
    private final TransactionEventPublisher publisher;

    public OutboxWorker(OutboxRepository outbox, TransactionEventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    public void dispatch() {
        for (OutboxRepository.Entry entry : outbox.claim(50, 600)) {
            try {
                TransactionEvent event = outbox.decode(entry);
                if (event.correlationId() != null) MDC.put("correlationId", event.correlationId());
                publisher.publish(event);
                outbox.published(entry.eventId(), entry.attempts());
            } catch (Exception exception) {
                log.warn("Outbox event {} delivery failed (attempt {})", entry.eventId(), entry.attempts(), exception);
                outbox.failed(entry.eventId(), entry.attempts(), exception.toString());
            } finally {
                MDC.remove("correlationId");
            }
        }
    }
}
