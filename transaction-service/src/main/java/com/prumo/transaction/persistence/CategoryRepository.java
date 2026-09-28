package com.prumo.transaction.persistence;

import com.prumo.transaction.domain.Category;
import com.prumo.transaction.domain.CategoryType;
import com.prumo.transaction.domain.TransactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbc;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Category category) {
        jdbc.update("INSERT INTO categories (id, owner_id, name, type) VALUES (?, ?, ?, ?)",
                category.id(), category.ownerId(), category.name(), category.type().name());
    }

    public List<Category> list(UUID ownerId) {
        return jdbc.query("SELECT id, owner_id, name, type FROM categories WHERE owner_id = ? ORDER BY name, id",
                (rs, row) -> new Category(rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class),
                        rs.getString("name"), CategoryType.valueOf(rs.getString("type"))), ownerId);
    }

    public Optional<Category> find(UUID ownerId, UUID id) {
        return jdbc.query("SELECT id, owner_id, name, type FROM categories WHERE owner_id = ? AND id = ?",
                (rs, row) -> new Category(rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class),
                        rs.getString("name"), CategoryType.valueOf(rs.getString("type"))), ownerId, id)
                .stream().findFirst();
    }

    public int update(UUID ownerId, UUID id, String name, CategoryType type) {
        return jdbc.update("UPDATE categories SET name = ?, type = ? WHERE owner_id = ? AND id = ?",
                name, type.name(), ownerId, id);
    }

    public int delete(UUID ownerId, UUID id) throws DataIntegrityViolationException {
        return jdbc.update("DELETE FROM categories WHERE owner_id = ? AND id = ?", ownerId, id);
    }

    public boolean supports(UUID ownerId, UUID id, TransactionType transactionType) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM categories
                WHERE owner_id = ? AND id = ? AND type IN ('BOTH', ?)
                """, Integer.class, ownerId, id, transactionType.name());
        return count != null && count > 0;
    }
}
