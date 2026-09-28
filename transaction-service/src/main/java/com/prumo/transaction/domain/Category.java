package com.prumo.transaction.domain;

import java.util.UUID;

public record Category(UUID id, UUID ownerId, String name, CategoryType type) {}
