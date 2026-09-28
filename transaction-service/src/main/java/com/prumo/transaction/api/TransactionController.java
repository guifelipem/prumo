package com.prumo.transaction.api;

import com.prumo.transaction.application.TransactionService;
import com.prumo.transaction.domain.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @PostMapping
    ResponseEntity<Transaction> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactions.create(authorization,
                request.accountId(), request.type(), request.amount(), request.description()));
    }

    @GetMapping
    List<Transaction> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                           @RequestParam UUID accountId,
                           @RequestParam(defaultValue = "0") @Min(0) int page,
                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return transactions.list(authorization, accountId, page, size);
    }
}
