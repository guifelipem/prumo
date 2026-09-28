package com.prumo.auth.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String tokenHash, UUID userId, Instant expiresAt) {
        jdbcTemplate.update("INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, ?)",
                tokenHash, userId, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    }

    public Optional<UUID> findActiveUserId(String tokenHash) {
        return jdbcTemplate.query(
                "SELECT user_id FROM sessions WHERE token_hash = ? AND expires_at > CURRENT_TIMESTAMP",
                (rs, rowNum) -> rs.getObject("user_id", UUID.class),
                tokenHash
        ).stream().findFirst();
    }
}
