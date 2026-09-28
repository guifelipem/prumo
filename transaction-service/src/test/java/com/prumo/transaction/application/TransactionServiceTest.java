package com.prumo.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

    private final AccountClient accounts = mock(AccountClient.class);
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionService service = new TransactionService(accounts, repository);

    @Test
    void recordsTransactionOnlyForAccountVisibleToToken() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);

        Transaction transaction = service.create("Bearer token", accountId, TransactionType.EXPENSE,
                new BigDecimal("25.50"), "  Mercado  ", null);

        assertEquals(accountId, transaction.accountId());
        assertEquals("Mercado", transaction.description());
        verify(accounts).requireOwner(accountId, "Bearer token");
        verify(repository).insert(transaction, ownerId);
    }

    @Test
    void preservesProvidedOccurrenceTime() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2024-05-03T12:30:00Z");
        when(accounts.requireOwner(accountId, "Bearer token")).thenReturn(ownerId);

        Transaction transaction = service.create("Bearer token", accountId, TransactionType.INCOME,
                new BigDecimal("10.00"), "Salário", occurredAt);

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
}
