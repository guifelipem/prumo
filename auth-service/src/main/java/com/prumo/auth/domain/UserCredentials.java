package com.prumo.auth.domain;

import java.util.UUID;

public record UserCredentials(UUID id, String passwordHash) {
}
