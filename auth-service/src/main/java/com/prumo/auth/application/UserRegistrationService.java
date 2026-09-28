package com.prumo.auth.application;

import com.prumo.auth.persistence.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserRegistrationService(UserRepository repository) {
        this.repository = repository;
    }

    public RegisteredUser register(String requestedEmail, String password) {
        String email = requestedEmail.trim().toLowerCase(Locale.ROOT);
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter no máximo 72 bytes em UTF-8.");
        }

        UUID id = UUID.randomUUID();
        try {
            repository.insert(id, email, passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
        return new RegisteredUser(id, email);
    }
}
