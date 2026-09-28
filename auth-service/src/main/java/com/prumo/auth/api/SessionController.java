package com.prumo.auth.api;

import com.prumo.auth.application.InvalidCredentialsException;
import com.prumo.auth.application.LoginResponse;
import com.prumo.auth.application.SessionService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final SessionService sessions;
    private final byte[] serviceKey;

    public SessionController(SessionService sessions, @Value("${app.internal-service-key}") String serviceKey) {
        if (serviceKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("INTERNAL_SERVICE_KEY deve ter pelo menos 32 bytes.");
        }
        this.sessions = sessions;
        this.serviceKey = serviceKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/sessions")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return sessions.login(request.email(), request.password());
    }

    @PostMapping("/internal/sessions/introspect")
    ResponseEntity<Map<String, UUID>> introspect(
            @RequestHeader(value = "X-Service-Key", required = false) String providedKey,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (providedKey == null || !MessageDigest.isEqual(serviceKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return sessions.introspect(authorization.substring(7))
                .map(id -> ResponseEntity.ok(Map.of("userId", id)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Void> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
