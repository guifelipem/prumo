package com.prumo.transaction.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.prumo.transaction.application.TransactionService;
import com.prumo.transaction.application.TransferService;
import com.prumo.transaction.domain.Category;
import com.prumo.transaction.domain.CategoryType;
import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.integration.IdentityClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@SpringBootTest(properties = "app.internal-service-key=12345678901234567890123456789012")
@Testcontainers
class TransactionPostgresIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TransactionRepository transactions;
    @Autowired CategoryRepository categories;
    @Autowired TransactionService service;
    @Autowired TransferService transferService;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AccountClient accounts;
    @MockitoBean IdentityClient identities;

    final UUID owner = UUID.randomUUID();
    final UUID other = UUID.randomUUID();
    final UUID account = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM transactions");
        jdbc.update("DELETE FROM transfers");
        jdbc.update("DELETE FROM categories");
        when(accounts.requireOwner(account, "Bearer owner")).thenReturn(owner);
        when(identities.requireUser("Bearer owner")).thenReturn(owner);
    }

    @Test
    void transferChangesBothBalancesAndCannotLoseOneLeg() {
        UUID destination = UUID.randomUUID();
        when(accounts.requireAccount(account, "Bearer owner"))
                .thenReturn(new AccountClient.OwnedAccount(owner, "BRL"));
        when(accounts.requireAccount(destination, "Bearer owner"))
                .thenReturn(new AccountClient.OwnedAccount(owner, "BRL"));
        when(accounts.requireOwner(destination, "Bearer owner")).thenReturn(owner);
        service.create("Bearer owner", account, TransactionType.INCOME,
                new BigDecimal("1000.00"), "Inicial", null, null);
        service.create("Bearer owner", destination, TransactionType.INCOME,
                new BigDecimal("500.00"), "Inicial", null, null);

        var transfer = transferService.create("Bearer owner", account, destination,
                new BigDecimal("300.00"), "Entre contas", null);
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("700.00")));
        assertEquals(0, service.balance("Bearer owner", destination).compareTo(new BigDecimal("800.00")));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM transactions WHERE transfer_id = ?",
                Integer.class, transfer.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "DELETE FROM transactions WHERE transfer_id = ? AND type = 'EXPENSE'", transfer.id()));

        transferService.update("Bearer owner", transfer.id(), account, destination,
                new BigDecimal("200.00"), "Corrigida", Instant.now());
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("800.00")));
        assertEquals(0, service.balance("Bearer owner", destination).compareTo(new BigDecimal("700.00")));
        transferService.delete("Bearer owner", transfer.id());
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("1000.00")));
        assertEquals(0, service.balance("Bearer owner", destination).compareTo(new BigDecimal("500.00")));
    }

    @Test
    void persistsListsUpdatesDeletesAndCalculatesBalance() {
        Transaction income = service.create("Bearer owner", account, TransactionType.INCOME,
                new BigDecimal("100.00"), " Salary ", Instant.parse("2024-01-01T00:00:00Z"), null);
        Transaction expense = service.create("Bearer owner", account, TransactionType.EXPENSE,
                new BigDecimal("25.50"), "Food", Instant.parse("2024-01-02T00:00:00Z"), null);
        assertEquals("Salary", transactions.findOwned(owner, income.id()).orElseThrow().description());
        assertEquals(2, service.list("Bearer owner", account, 0, 10).size());
        assertEquals(0, transactions.listForAccount(other, account, 0, 10).size());
        assertEquals(0, transactions.findOwned(other, income.id()).stream().count());
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("74.50")));

        service.update("Bearer owner", expense.id(), TransactionType.EXPENSE,
                new BigDecimal("10.00"), "Updated", Instant.parse("2024-01-03T00:00:00Z"), null);
        assertEquals("Updated", transactions.findOwned(owner, expense.id()).orElseThrow().description());
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("90.00")));
        service.delete("Bearer owner", expense.id());
        assertTrue(transactions.findOwned(owner, expense.id()).isEmpty());
        assertEquals(0, service.balance("Bearer owner", account).compareTo(new BigDecimal("100.00")));
    }

    @Test
    void rejectsAnotherOwnersCategoryAndEnforcesDatabaseConstraints() {
        UUID categoryId = UUID.randomUUID();
        categories.insert(new Category(categoryId, other, "Private", CategoryType.EXPENSE));
        assertThrows(ResponseStatusException.class, () -> service.create("Bearer owner", account,
                TransactionType.EXPENSE, new BigDecimal("1.00"), "Test", Instant.now(), categoryId));
        assertEquals(0, transactions.listForAccount(owner, account, 0, 10).size());

        Transaction invalid = new Transaction(UUID.randomUUID(), account, TransactionType.EXPENSE,
                new BigDecimal("0.00"), "Invalid", Instant.now(), null, Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> transactions.insert(invalid, owner));
        Transaction foreignCategory = new Transaction(UUID.randomUUID(), account, TransactionType.EXPENSE,
                new BigDecimal("1.00"), "Invalid", Instant.now(), categoryId, Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> transactions.insert(foreignCategory, owner));
    }
}
