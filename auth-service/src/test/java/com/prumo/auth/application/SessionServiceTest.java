package com.prumo.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prumo.auth.domain.UserCredentials;
import com.prumo.auth.persistence.SessionRepository;
import com.prumo.auth.persistence.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SessionServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final SessionService service = new SessionService(users, sessions);

    @Test
    void loginStoresTokenHashAndReturnsExpiringToken() {
        UUID userId = UUID.randomUUID();
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(
                new UserCredentials(userId, new BCryptPasswordEncoder().encode("correct horse battery"))));

        LoginResponse result = service.login(" Ana@Example.COM ", "correct horse battery");

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        verify(sessions).insert(storedHash.capture(), eq(userId), any(Instant.class));
        assertEquals(64, storedHash.getValue().length());
        assertFalse(storedHash.getValue().equals(result.accessToken()));
        assertTrue(result.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void wrongPasswordDoesNotCreateSession() {
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(
                new UserCredentials(UUID.randomUUID(), new BCryptPasswordEncoder().encode("correct horse battery"))));

        assertThrows(InvalidCredentialsException.class,
                () -> service.login("ana@example.com", "incorrect password"));
    }
}
