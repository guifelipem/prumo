package com.prumo.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.persistence.TransactionRepository;
import java.math.BigDecimal;
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
                new BigDecimal("25.50"), "  Mercado  ");

        assertEquals(accountId, transaction.accountId());
        assertEquals("Mercado", transaction.description());
        verify(accounts).requireOwner(accountId, "Bearer token");
        verify(repository).insert(transaction, ownerId);
    }
}
