package com.prumo.transaction.application;

import com.prumo.transaction.domain.Transaction;
import com.prumo.transaction.domain.TransactionType;
import com.prumo.transaction.domain.Transfer;
import com.prumo.transaction.integration.AccountClient;
import com.prumo.transaction.integration.IdentityClient;
import com.prumo.transaction.persistence.TransactionRepository;
import com.prumo.transaction.persistence.TransferRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferService {
    private final AccountClient accounts;
    private final IdentityClient identities;
    private final TransactionRepository transactions;
    private final TransferRepository transfers;

    public TransferService(AccountClient accounts, IdentityClient identities,
                           TransactionRepository transactions, TransferRepository transfers) {
        this.accounts = accounts;
        this.identities = identities;
        this.transactions = transactions;
        this.transfers = transfers;
    }

    @Transactional
    public Transfer create(String authorization, UUID source, UUID destination, BigDecimal amount,
                           String description, Instant occurredAt) {
        UUID ownerId = validateAccounts(authorization, source, destination);
        validateAmount(amount);
        Instant now = Instant.now();
        Transfer transfer = new Transfer(UUID.randomUUID(), ownerId, source, destination, amount,
                description.trim(), occurredAt == null ? now : occurredAt, now);
        transfers.insert(transfer);
        transactions.insert(leg(transfer, source, TransactionType.EXPENSE), ownerId);
        transactions.insert(leg(transfer, destination, TransactionType.INCOME), ownerId);
        return transfer;
    }

    public Transfer get(String authorization, UUID id) {
        UUID ownerId = identities.requireUser(authorization);
        return transfers.findOwned(ownerId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Transfer> list(String authorization, int page, int size) {
        return transfers.listOwned(identities.requireUser(authorization), (long) page * size, size);
    }

    @Transactional
    public Transfer update(String authorization, UUID id, UUID source, UUID destination,
                           BigDecimal amount, String description, Instant occurredAt) {
        Transfer current = get(authorization, id);
        UUID ownerId = validateAccounts(authorization, source, destination);
        if (!current.ownerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        validateAmount(amount);
        Transfer updated = new Transfer(id, ownerId, source, destination, amount,
                description.trim(), occurredAt == null ? current.occurredAt() : occurredAt, current.createdAt());
        if (transfers.update(updated) != 1
                || transfers.updateLeg(ownerId, id, "EXPENSE", source, updated) != 1
                || transfers.updateLeg(ownerId, id, "INCOME", destination, updated) != 1) {
            throw new IllegalStateException("Transferência sem as duas movimentações");
        }
        return updated;
    }

    @Transactional
    public void delete(String authorization, UUID id) {
        Transfer transfer = get(authorization, id);
        if (transfers.deleteLegs(transfer.ownerId(), id) != 2
                || transfers.delete(transfer.ownerId(), id) != 1) {
            throw new IllegalStateException("Transferência sem as duas movimentações");
        }
    }

    private UUID validateAccounts(String authorization, UUID source, UUID destination) {
        if (source.equals(destination)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contas de origem e destino devem ser diferentes");
        }
        AccountClient.OwnedAccount from = accounts.requireAccount(source, authorization);
        AccountClient.OwnedAccount to = accounts.requireAccount(destination, authorization);
        if (!from.ownerId().equals(to.ownerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (from.currency() == null || !from.currency().equals(to.currency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moedas incompatíveis");
        }
        return from.ownerId();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor deve ser positivo");
        }
    }

    private Transaction leg(Transfer transfer, UUID accountId, TransactionType type) {
        return new Transaction(UUID.randomUUID(), accountId, type, transfer.amount(),
                transfer.description(), transfer.occurredAt(), null, transfer.createdAt(), transfer.id());
    }
}
