package com.prumo.account.domain;

import java.util.UUID;

public record Account(UUID id, UUID ownerId, String name, String currency) {
}
