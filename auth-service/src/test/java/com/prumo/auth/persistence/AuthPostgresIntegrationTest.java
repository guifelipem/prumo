package com.prumo.auth.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
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

@SpringBootTest(properties = "app.internal-service-key=12345678901234567890123456789012")
@Testcontainers
class AuthPostgresIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository users;
    @Autowired SessionRepository sessions;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clean() {
        jdbc.update("DELETE FROM sessions");
        jdbc.update("DELETE FROM users");
    }

    @Test void persistsCredentialsAndOnlyFindsActiveSessions() {
        UUID id = UUID.randomUUID();
        users.insert(id, "user@example.com", "x".repeat(60));
        assertEquals(id, users.findByEmail("user@example.com").orElseThrow().id());
        sessions.insert("a".repeat(64), id, Instant.now().plusSeconds(3600));
        sessions.insert("b".repeat(64), id, Instant.now().minusSeconds(3600));
        assertEquals(id, sessions.findActiveUserId("a".repeat(64)).orElseThrow());
        assertTrue(sessions.findActiveUserId("b".repeat(64)).isEmpty());
    }

    @Test void migrationEnforcesUniqueEmailAndSessionForeignKey() {
        users.insert(UUID.randomUUID(), "user@example.com", "x".repeat(60));
        assertThrows(DataIntegrityViolationException.class, () ->
                users.insert(UUID.randomUUID(), "user@example.com", "x".repeat(60)));
        assertThrows(DataIntegrityViolationException.class, () ->
                sessions.insert("c".repeat(64), UUID.randomUUID(), Instant.now().plusSeconds(3600)));
    }
}
