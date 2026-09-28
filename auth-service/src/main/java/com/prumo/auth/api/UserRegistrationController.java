package com.prumo.auth.api;

import com.prumo.auth.application.EmailAlreadyRegisteredException;
import com.prumo.auth.application.RegisteredUser;
import com.prumo.auth.application.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserRegistrationController {

    private final UserRegistrationService registrationService;

    public UserRegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    ResponseEntity<RegisteredUser> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisteredUser user = registrationService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<Void> duplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> invalidPassword() {
        return ResponseEntity.badRequest().build();
    }
}
