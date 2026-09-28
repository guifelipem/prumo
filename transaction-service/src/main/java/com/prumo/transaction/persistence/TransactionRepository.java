package com.prumo.transaction.persistence;

import com.prumo.transaction.domain.Transaction;
import java.util.UUID;
import java.util.List;
import com.prumo.transaction.domain.TransactionType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Transaction transaction, UUID ownerId) {
        jdbcTemplate.update("""
                INSERT INTO transactions (id, owner_id, account_id, type, amount, description, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, transaction.id(), ownerId, transaction.accountId(), transaction.type().name(),
                transaction.amount(), transaction.description(),
                OffsetDateTime.ofInstant(transaction.occurredAt(), ZoneOffset.UTC));
    }

    public List<Transaction> listForAccount(UUID ownerId, UUID accountId, long offset, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, account_id, type, amount, description, occurred_at FROM transactions
                WHERE owner_id = ? AND account_id = ?
                ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> new Transaction(
                        rs.getObject("id", UUID.class),
                        rs.getObject("account_id", UUID.class),
                        TransactionType.valueOf(rs.getString("type")),
                        rs.getBigDecimal("amount"),
                        rs.getString("description"),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant()),
                ownerId, accountId, limit, offset
        );
    }
}
