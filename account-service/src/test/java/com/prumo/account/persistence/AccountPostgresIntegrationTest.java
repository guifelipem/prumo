package com.prumo.account.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.prumo.account.domain.Account;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "app.internal-service-key=test-key")
@Testcontainers
class AccountPostgresIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired AccountRepository accounts;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clean() { jdbc.update("DELETE FROM accounts"); }

    @Test void persistsAndIsolatesAccountsByOwner() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Account account = new Account(UUID.randomUUID(), owner, "Checking", "BRL");
        accounts.insert(account);
        assertEquals(account, accounts.findOwned(account.id(), owner).orElseThrow());
        assertTrue(accounts.findOwned(account.id(), other).isEmpty());
        assertEquals(1, accounts.listOwned(owner, 0, 10).size());
        assertTrue(accounts.listOwned(other, 0, 10).isEmpty());
    }

    @Test void migrationRejectsMissingOwner() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO accounts (id, name, currency) VALUES (?, ?, ?)", UUID.randomUUID(), "Invalid", "BRL"));
    }
}
