package com.prumo.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.persistence.OutboxRepository;
import com.prumo.transaction.persistence.TransactionRepository;
import com.prumo.transaction.persistence.CategoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

    private final AccountClient accounts = mock(AccountClient.class);
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final IdentityClient identities = mock(IdentityClient.class);
    private final OutboxRepository events = mock(OutboxRepository.class);
    private final TransactionService service = new TransactionService(accounts, repository, categories, identities, events);

    @Test
    void recordsTransactionOnlyForAccountVisibleToToken() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);

        Transaction transaction = service.create("Bearer token", accountId, TransactionType.EXPENSE,
                new BigDecimal("25.50"), "  Mercado  ", null, null);

        assertEquals(accountId, transaction.accountId());
        assertEquals("Mercado", transaction.description());
        verify(accounts).requireOwner(accountId, "Bearer token");
        verify(repository).insert(transaction, ownerId);
        verify(events).save(org.mockito.ArgumentMatchers.argThat(event ->
                event.eventType().equals("TransactionCreated") && event.transactionId().equals(transaction.id())));
    }

    @Test
    void preservesProvidedOccurrenceTime() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2024-05-03T12:30:00Z");
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);

        Transaction transaction = service.create("Bearer token", accountId, TransactionType.INCOME,
                new BigDecimal("10.00"), "Salário", occurredAt, null);

        assertEquals(occurredAt, transaction.occurredAt());
        verify(repository).insert(transaction, ownerId);
    }

    @Test
    void getsBalanceOnlyAfterCheckingAccountAccess() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);
        when(repository.balanceForAccount(ownerId, accountId)).thenReturn(new BigDecimal("-15.50"));

        assertEquals(new BigDecimal("-15.50"), service.balance("Bearer token", accountId));
        verify(accounts).requireOwner(accountId, "Bearer token");
        verify(repository).balanceForAccount(ownerId, accountId);
    }

    @Test
    void doesNotQueryBalanceWhenAccountAccessFails() {
        UUID accountId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer other"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class,
                () -> service.balance("Bearer other", accountId));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsCategoryNotOwnedOrNotCompatibleWithTransactionType() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create("Bearer token", accountId, TransactionType.EXPENSE,
                        new BigDecimal("80.00"), "Mercado", null, categoryId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(categories).supports(ownerId, categoryId, TransactionType.EXPENSE);
        verifyNoInteractions(repository);
    }

    @Test
    void recordsTransactionWithOwnedCompatibleCategory() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);
        when(categories.supports(ownerId, categoryId, TransactionType.EXPENSE)).thenReturn(true);

        Transaction transaction = service.create("Bearer token", accountId, TransactionType.EXPENSE,
                new BigDecimal("80.00"), "Mercado", null, categoryId);

        assertEquals(categoryId, transaction.categoryId());
        verify(repository).insert(transaction, ownerId);
    }

    @Test
    void updatesOwnedTransactionWithoutChangingAccountOrCreationTime() {
        UUID ownerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");
        Instant occurredAt = Instant.parse("2024-05-03T12:30:00Z");
        Transaction current = new Transaction(transactionId, accountId, TransactionType.EXPENSE,
                new BigDecimal("80.00"), "Mercado", createdAt, null, createdAt);
        when(identities.requireUser("Bearer token")).thenReturn(ownerId);
        when(repository.findOwned(ownerId, transactionId)).thenReturn(Optional.of(current));
        when(categories.supports(ownerId, categoryId, TransactionType.INCOME)).thenReturn(true);
        when(repository.update(org.mockito.ArgumentMatchers.eq(ownerId), org.mockito.ArgumentMatchers.any(Transaction.class)))
                .thenReturn(1);

        Transaction updated = service.update("Bearer token", transactionId, TransactionType.INCOME,
                new BigDecimal("100.00"), "  Reembolso  ", occurredAt, categoryId);

        assertEquals(accountId, updated.accountId());
        assertEquals(createdAt, updated.createdAt());
        assertEquals(occurredAt, updated.occurredAt());
        assertEquals("Reembolso", updated.description());
        verify(repository).update(ownerId, updated);
        verify(events).save(org.mockito.ArgumentMatchers.argThat(event ->
                event.eventType().equals("TransactionUpdated") && event.transaction().equals(updated)));
    }

    @Test
    void cannotUpdateOrDeleteAnotherUsersTransaction() {
        UUID ownerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        when(identities.requireUser("Bearer other")).thenReturn(ownerId);

        ResponseStatusException update = assertThrows(ResponseStatusException.class,
                () -> service.update("Bearer other", transactionId, TransactionType.EXPENSE,
                        new BigDecimal("1.00"), "Teste", Instant.now(), null));
        ResponseStatusException delete = assertThrows(ResponseStatusException.class,
                () -> service.delete("Bearer other", transactionId));

        assertEquals(HttpStatus.NOT_FOUND, update.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, delete.getStatusCode());
        verify(repository, org.mockito.Mockito.times(2)).findOwned(ownerId, transactionId);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(ownerId, transactionId);
        verifyNoInteractions(events);
    }

    @Test
    void cannotUpdateWithAnotherUsersCategory() {
        UUID ownerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction current = new Transaction(transactionId, UUID.randomUUID(), TransactionType.EXPENSE,
                new BigDecimal("80.00"), "Mercado", Instant.now(), null, Instant.now());
        when(identities.requireUser("Bearer token")).thenReturn(ownerId);
        when(repository.findOwned(ownerId, transactionId)).thenReturn(Optional.of(current));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update("Bearer token", transactionId, TransactionType.EXPENSE,
                        new BigDecimal("50.00"), "Mercado", Instant.now(), UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishesDeletedTransactionAfterRemoval() {
        UUID ownerId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Transaction current = new Transaction(id, UUID.randomUUID(), TransactionType.EXPENSE,
                new BigDecimal("12.00"), "Café", Instant.now(), null, Instant.now());
        when(identities.requireUser("Bearer token")).thenReturn(ownerId);
        when(repository.findOwned(ownerId, id)).thenReturn(Optional.of(current));
        when(repository.delete(ownerId, id)).thenReturn(1);

        service.delete("Bearer token", id);

        verify(events).save(org.mockito.ArgumentMatchers.argThat(event ->
                event.eventType().equals("TransactionDeleted") && event.transaction().equals(current)));
    }
}
