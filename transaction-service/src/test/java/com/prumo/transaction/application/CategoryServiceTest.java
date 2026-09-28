package com.prumo.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prumo.transaction.domain.Category;
import com.prumo.transaction.domain.CategoryType;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.persistence.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

class CategoryServiceTest {
    private final IdentityClient identities = mock(IdentityClient.class);
    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryService service = new CategoryService(identities, repository);

    @Test
    void createsAndListsOnlyForAuthenticatedOwner() {
        UUID ownerId = UUID.randomUUID();
        when(identities.requireUser("Bearer token")).thenReturn(ownerId);

        Category category = service.create("Bearer token", "  Alimentação  ", null);
        when(repository.list(ownerId)).thenReturn(List.of(category));

        assertEquals(ownerId, category.ownerId());
        assertEquals("Alimentação", category.name());
        assertEquals(CategoryType.BOTH, category.type());
        assertEquals(List.of(category), service.list("Bearer token"));
        verify(repository).insert(category);
    }

    @Test
    void cannotUpdateOrDeleteAnotherOwnersCategory() {
        UUID ownerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(identities.requireUser("Bearer other")).thenReturn(ownerId);

        ResponseStatusException update = assertThrows(ResponseStatusException.class,
                () -> service.update("Bearer other", categoryId, "Novo nome", CategoryType.EXPENSE));
        ResponseStatusException delete = assertThrows(ResponseStatusException.class,
                () -> service.delete("Bearer other", categoryId));

        assertEquals(HttpStatus.NOT_FOUND, update.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, delete.getStatusCode());
        verify(repository).find(ownerId, categoryId);
        verify(repository).delete(ownerId, categoryId);
    }

    @Test
    void updatesOwnedCategoryAndRejectsDeletingOneInUse() {
        UUID ownerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(identities.requireUser("Bearer token")).thenReturn(ownerId);
        when(repository.find(ownerId, categoryId))
                .thenReturn(java.util.Optional.of(new Category(categoryId, ownerId, "Antigo", CategoryType.BOTH)));
        when(repository.update(ownerId, categoryId, "Mercado", CategoryType.EXPENSE)).thenReturn(1);
        when(repository.delete(ownerId, categoryId))
                .thenThrow(new DataIntegrityViolationException("Categoria em uso"));

        Category updated = service.update("Bearer token", categoryId, " Mercado ", CategoryType.EXPENSE);
        ResponseStatusException delete = assertThrows(ResponseStatusException.class,
                () -> service.delete("Bearer token", categoryId));

        assertEquals("Mercado", updated.name());
        assertEquals(CategoryType.EXPENSE, updated.type());
        assertEquals(HttpStatus.CONFLICT, delete.getStatusCode());
    }
}
