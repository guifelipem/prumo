package com.prumo.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.prumo.transaction.domain.Transfer;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.persistence.TransactionRepository;
import com.prumo.transaction.persistence.TransferRepository;
import com.prumo.transaction.persistence.OutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TransferServiceTest {
    private final AccountClient accounts = mock(AccountClient.class);
    private final IdentityClient identities = mock(IdentityClient.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final TransferRepository transfers = mock(TransferRepository.class);
    private final OutboxRepository events = mock(OutboxRepository.class);
    private final TransferService service = new TransferService(accounts, identities, transactions, transfers, events);
    private final UUID owner = UUID.randomUUID();
    private final UUID source = UUID.randomUUID();
    private final UUID destination = UUID.randomUUID();

    @Test
    void createsLinkedExpenseAndIncomeForSameOwnerAndCurrency() {
        visibleAccounts();
        Transfer transfer = service.create("Bearer token", source, destination,
                new BigDecimal("300.00"), "  Reserva  ", null);

        assertEquals("Reserva", transfer.description());
        verify(transfers).insert(transfer);
        verify(transactions).insert(org.mockito.ArgumentMatchers.argThat(leg ->
                leg.transferId().equals(transfer.id()) && leg.accountId().equals(source)
                        && leg.type().name().equals("EXPENSE") && leg.amount().equals(transfer.amount())),
                org.mockito.ArgumentMatchers.eq(owner));
        verify(transactions).insert(org.mockito.ArgumentMatchers.argThat(leg ->
                leg.transferId().equals(transfer.id()) && leg.accountId().equals(destination)
                        && leg.type().name().equals("INCOME") && leg.amount().equals(transfer.amount())),
                org.mockito.ArgumentMatchers.eq(owner));
    }

    @Test
    void rejectsSameAccountAndIncompatibleCurrency() {
        ResponseStatusException same = assertThrows(ResponseStatusException.class,
                () -> service.create("Bearer token", source, source,
                        BigDecimal.ONE, "Teste", null));
        assertEquals(HttpStatus.BAD_REQUEST, same.getStatusCode());
        verifyNoInteractions(transfers, transactions);

        when(accounts.requireAccount(source, "Bearer token"))
                .thenReturn(new AccountClient.OwnedAccount(owner, "BRL"));
        when(accounts.requireAccount(destination, "Bearer token"))
                .thenReturn(new AccountClient.OwnedAccount(owner, "USD"));
        ResponseStatusException currency = assertThrows(ResponseStatusException.class,
                () -> service.create("Bearer token", source, destination,
                        BigDecimal.ONE, "Teste", null));
        assertEquals(HttpStatus.BAD_REQUEST, currency.getStatusCode());
        verifyNoInteractions(transfers, transactions);
    }

    @Test
    void updatesBothLegsAndDeletesBothLegs() {
        visibleAccounts();
        UUID id = UUID.randomUUID();
        Transfer existing = new Transfer(id, owner, source, destination, BigDecimal.ONE,
                "Antes", Instant.now(), Instant.now());
        when(identities.requireUser("Bearer token")).thenReturn(owner);
        when(transfers.findOwned(owner, id)).thenReturn(Optional.of(existing));
        when(transfers.update(any())).thenReturn(1);
        when(transfers.updateLeg(org.mockito.ArgumentMatchers.eq(owner), org.mockito.ArgumentMatchers.eq(id),
                any(), any(), any())).thenReturn(1);
        when(transfers.deleteLegs(owner, id)).thenReturn(2);
        when(transfers.delete(owner, id)).thenReturn(1);

        Transfer changed = service.update("Bearer token", id, source, destination,
                new BigDecimal("42.00"), "Depois", Instant.now());
        assertEquals(existing.createdAt(), changed.createdAt());
        verify(transfers).updateLeg(owner, id, "EXPENSE", source, changed);
        verify(transfers).updateLeg(owner, id, "INCOME", destination, changed);
        service.delete("Bearer token", id);
        verify(transfers).deleteLegs(owner, id);
        verify(transfers).delete(owner, id);
    }
}
