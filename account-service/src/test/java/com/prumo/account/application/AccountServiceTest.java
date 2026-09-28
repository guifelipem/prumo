package com.prumo.account.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.prumo.account.domain.Account;
import com.prumo.account.persistence.AccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final AccountRepository repository = mock(AccountRepository.class);
    private final AccountService service = new AccountService(repository);

    @Test
    void createsAccountOwnedByAuthenticatedUser() {
        UUID ownerId = UUID.randomUUID();

        Account account = service.create(ownerId, "  Reserva  ", "brl");

        assertEquals(ownerId, account.ownerId());
        assertEquals("Reserva", account.name());
        assertEquals("BRL", account.currency());
        verify(repository).insert(account);
    }

    @Test
    void rejectsInvalidCurrencyBeforeWriting() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(UUID.randomUUID(), "Reserva", "ZZZ"));
        verifyNoInteractions(repository);
    }
}
