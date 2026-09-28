package com.prumo.auth.persistence;

import com.prumo.auth.domain.UserCredentials;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID id, String email, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
                id, email, passwordHash
        );
    }

    public Optional<UserCredentials> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, password_hash FROM users WHERE email = ?",
                (rs, rowNum) -> new UserCredentials(rs.getObject("id", UUID.class), rs.getString("password_hash")),
                email
        ).stream().findFirst();
    }
}
