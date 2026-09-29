package com.prumo.transaction.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prumo.transaction.integration.TransactionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxRepository {
    public record Entry(UUID eventId, String payload, int attempts) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void save(TransactionEvent event) {
        try {
            jdbc.update("INSERT INTO outbox_events (event_id, event_type, payload) VALUES (?, ?, ?::jsonb)",
                    event.eventId(), event.eventType(), mapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar evento", exception);
        }
    }

    // The row lock makes claiming safe across concurrent workers and instances.
    @Transactional
    public List<Entry> claim(int size, int leaseSeconds) {
        return jdbc.query("""
                WITH ready AS (
                    SELECT event_id FROM outbox_events
                    WHERE published_at IS NULL AND next_attempt_at <= now()
                      AND (claimed_until IS NULL OR claimed_until < now())
                    ORDER BY created_at, event_id
                    LIMIT ? FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events o
                SET claimed_until = now() + (? * interval '1 second'), attempts = attempts + 1
                FROM ready WHERE o.event_id = ready.event_id
                RETURNING o.event_id, o.payload::text AS payload, o.attempts
                """, (rs, row) -> map(rs), size, leaseSeconds);
    }

    public void published(UUID id, int attempts) {
        jdbc.update("""
                UPDATE outbox_events SET published_at = now(), claimed_until = NULL, last_error = NULL
                WHERE event_id = ? AND attempts = ? AND published_at IS NULL
                """, id, attempts);
    }

    public void failed(UUID id, int attempts, String error) {
        long delay = Math.min(3600L, 1L << Math.min(12, attempts));
        jdbc.update("""
                UPDATE outbox_events
                SET claimed_until = NULL, next_attempt_at = now() + (? * interval '1 second'),
                    last_error = ?
                WHERE event_id = ? AND attempts = ? AND published_at IS NULL
                """, delay, error.substring(0, Math.min(1000, error.length())), id, attempts);
    }

    public TransactionEvent decode(Entry entry) {
        try {
            return mapper.readValue(entry.payload(), TransactionEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Evento da outbox inválido: " + entry.eventId(), exception);
        }
    }

    private Entry map(ResultSet rs) throws SQLException {
        return new Entry(rs.getObject("event_id", UUID.class), rs.getString("payload"), rs.getInt("attempts"));
    }
}
