package com.prumo.transaction.application;

import com.prumo.transaction.domain.Category;
import com.prumo.transaction.domain.CategoryType;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.persistence.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {
    private final IdentityClient identities;
    private final CategoryRepository repository;

    public CategoryService(IdentityClient identities, CategoryRepository repository) {
        this.identities = identities;
        this.repository = repository;
    }

    public Category create(String authorization, String name, CategoryType type) {
        UUID ownerId = identities.requireUser(authorization);
        Category category = new Category(UUID.randomUUID(), ownerId, name.trim(), type == null ? CategoryType.BOTH : type);
        repository.insert(category);
        return category;
    }

    public List<Category> list(String authorization) {
        return repository.list(identities.requireUser(authorization));
    }

    public Category update(String authorization, UUID id, String name, CategoryType type) {
        UUID ownerId = identities.requireUser(authorization);
        Category current = repository.find(ownerId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CategoryType updatedType = type == null ? CategoryType.BOTH : type;
        if (repository.update(ownerId, id, name.trim(), updatedType) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new Category(id, current.ownerId(), name.trim(), updatedType);
    }

    public void delete(String authorization, UUID id) {
        UUID ownerId = identities.requireUser(authorization);
        try {
            if (repository.delete(ownerId, id) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria em uso", exception);
        }
    }
}
