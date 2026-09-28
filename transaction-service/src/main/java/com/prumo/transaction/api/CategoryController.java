package com.prumo.transaction.api;

import com.prumo.transaction.application.CategoryService;
import com.prumo.transaction.domain.Category;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @PostMapping
    ResponseEntity<Category> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categories.create(authorization, request.name(), request.type()));
    }

    @GetMapping
    List<Category> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return categories.list(authorization);
    }

    @PutMapping("/{id}")
    Category update(@RequestHeader(value = "Authorization", required = false) String authorization,
                    @PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return categories.update(authorization, id, request.name(), request.type());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable UUID id) {
        categories.delete(authorization, id);
        return ResponseEntity.noContent().build();
    }
}
