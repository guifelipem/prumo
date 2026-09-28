package com.prumo.account.api;

import com.prumo.account.application.AccountService;
import com.prumo.account.domain.Account;
import com.prumo.account.integration.IdentityClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final IdentityClient identities;
    private final AccountService accounts;

    public AccountController(IdentityClient identities, AccountService accounts) {
        this.identities = identities;
        this.accounts = accounts;
    }

    @PostMapping
    ResponseEntity<Account> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @Valid @RequestBody CreateAccountRequest request) {
        UUID ownerId = identities.requireUser(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.create(ownerId, request.name(), request.currency()));
    }

    @GetMapping("/{id}")
    Account get(@RequestHeader(value = "Authorization", required = false) String authorization,
                @PathVariable UUID id) {
        UUID ownerId = identities.requireUser(authorization);
        return accounts.findOwned(id, ownerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    List<Account> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @RequestParam(defaultValue = "0") @Min(0) int page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        UUID ownerId = identities.requireUser(authorization);
        return accounts.listOwned(ownerId, page, size);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> invalidCurrency() {
        return ResponseEntity.badRequest().build();
    }
}
