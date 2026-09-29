package com.prumo.transaction.integration;

import static org.mockito.Mockito.*;

import com.prumo.transaction.persistence.OutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxWorkerTest {
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final TransactionEventPublisher publisher = mock(TransactionEventPublisher.class);
    private final OutboxWorker worker = new OutboxWorker(outbox, publisher);

    @Test
    void confirmsDeliveryBeforeMarkingPublished() {
        var entry = new OutboxRepository.Entry(UUID.randomUUID(), "{}", 1);
        var event = mock(TransactionEvent.class);
        when(outbox.claim(50, 600)).thenReturn(List.of(entry));
        when(outbox.decode(entry)).thenReturn(event);

        worker.dispatch();

        var order = inOrder(publisher, outbox);
        order.verify(publisher).publish(event);
        order.verify(outbox).published(entry.eventId(), 1);
        verify(outbox, never()).failed(any(), anyInt(), anyString());
    }

    @Test
    void publicationFailureLeavesEventForRetry() {
        var entry = new OutboxRepository.Entry(UUID.randomUUID(), "{}", 2);
        var event = mock(TransactionEvent.class);
        when(outbox.claim(50, 600)).thenReturn(List.of(entry));
        when(outbox.decode(entry)).thenReturn(event);
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(event);

        worker.dispatch();

        verify(outbox).failed(eq(entry.eventId()), eq(2), contains("broker unavailable"));
        verify(outbox, never()).published(any(), anyInt());
    }
}
