package com.prumo.auth.application;

import java.util.UUID;

public record RegisteredUser(UUID id, String email) {
}
