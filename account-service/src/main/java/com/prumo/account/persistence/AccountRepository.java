package com.prumo.account.persistence;

import com.prumo.account.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Account account) {
        jdbcTemplate.update("INSERT INTO accounts (id, owner_id, name, currency) VALUES (?, ?, ?, ?)",
                account.id(), account.ownerId(), account.name(), account.currency());
    }

    public Optional<Account> findOwned(UUID id, UUID ownerId) {
        return jdbcTemplate.query(
                "SELECT id, owner_id, name, currency FROM accounts WHERE id = ? AND owner_id = ?",
                (rs, rowNum) -> new Account(rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class),
                        rs.getString("name"), rs.getString("currency").trim()),
                id, ownerId
        ).stream().findFirst();
    }

    public List<Account> listOwned(UUID ownerId, long offset, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, owner_id, name, currency FROM accounts
                WHERE owner_id = ? ORDER BY created_at, id LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> new Account(rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class),
                        rs.getString("name"), rs.getString("currency").trim()),
                ownerId, limit, offset
        );
    }
}
