package com.prumo.transaction.api;

import com.prumo.transaction.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(@NotBlank @Size(max = 100) String name, CategoryType type) {}
