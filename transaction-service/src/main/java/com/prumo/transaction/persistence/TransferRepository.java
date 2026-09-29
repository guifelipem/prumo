package com.prumo.transaction.persistence;

import com.prumo.transaction.domain.Transfer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransferRepository {
    private final JdbcTemplate jdbc;

    public TransferRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Transfer transfer) {
        jdbc.update("""
                INSERT INTO transfers (id, owner_id, source_account_id, destination_account_id,
                                       amount, description, occurred_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, transfer.id(), transfer.ownerId(), transfer.sourceAccountId(), transfer.destinationAccountId(),
                transfer.amount(), transfer.description(), time(transfer.occurredAt()), time(transfer.createdAt()));
    }

    public Optional<Transfer> findOwned(UUID ownerId, UUID id) {
        return jdbc.query("SELECT * FROM transfers WHERE owner_id = ? AND id = ?",
                (rs, row) -> map(rs), ownerId, id).stream().findFirst();
    }

    public List<Transfer> listOwned(UUID ownerId, long offset, int limit) {
        return jdbc.query("""
                SELECT * FROM transfers WHERE owner_id = ?
                ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> map(rs), ownerId, limit, offset);
    }

    public int update(Transfer transfer) {
        return jdbc.update("""
                UPDATE transfers SET source_account_id = ?, destination_account_id = ?, amount = ?,
                                     description = ?, occurred_at = ? WHERE owner_id = ? AND id = ?
                """, transfer.sourceAccountId(), transfer.destinationAccountId(), transfer.amount(),
                transfer.description(), time(transfer.occurredAt()), transfer.ownerId(), transfer.id());
    }

    public int updateLeg(UUID ownerId, UUID transferId, String type, UUID accountId, Transfer transfer) {
        return jdbc.update("""
                UPDATE transactions SET account_id = ?, amount = ?, description = ?, occurred_at = ?
                WHERE owner_id = ? AND transfer_id = ? AND type = ?
                """, accountId, transfer.amount(), transfer.description(), time(transfer.occurredAt()),
                ownerId, transferId, type);
    }

    public int deleteLegs(UUID ownerId, UUID transferId) {
        return jdbc.update("DELETE FROM transactions WHERE owner_id = ? AND transfer_id = ?", ownerId, transferId);
    }

    public int delete(UUID ownerId, UUID id) {
        return jdbc.update("DELETE FROM transfers WHERE owner_id = ? AND id = ?", ownerId, id);
    }

    private static OffsetDateTime time(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Transfer map(ResultSet rs) throws SQLException {
        return new Transfer(rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class),
                rs.getObject("source_account_id", UUID.class), rs.getObject("destination_account_id", UUID.class),
                rs.getBigDecimal("amount"), rs.getString("description"),
                rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
