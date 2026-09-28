package com.prumo.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.prumo.auth.persistence.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserRegistrationServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final UserRegistrationService service = new UserRegistrationService(repository);

    @Test
    void normalizesEmailAndStoresOnlyPasswordHash() {
        RegisteredUser user = service.register("  Ana@Example.COM  ", "correct horse battery");

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).insert(eq(user.id()), eq("ana@example.com"), hash.capture());
        assertEquals("ana@example.com", user.email());
        assertTrue(new BCryptPasswordEncoder().matches("correct horse battery", hash.getValue()));
    }

    @Test
    void rejectsDuplicateEmail() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(repository).insert(any(UUID.class), eq("ana@example.com"), any(String.class));

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.register("ana@example.com", "correct horse battery"));
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("ana@example.com", "á".repeat(37)));
    }
}
