package com.prumo.auth.application;

import java.time.Instant;

public record LoginResponse(String accessToken, Instant expiresAt) {
}
