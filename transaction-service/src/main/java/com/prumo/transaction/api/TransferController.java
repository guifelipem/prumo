package com.prumo.transaction.api;

import com.prumo.transaction.application.TransferService;
import com.prumo.transaction.domain.Transfer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    ResponseEntity<Transfer> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transfers.create(authorization,
                request.sourceAccountId(), request.destinationAccountId(), request.amount(),
                request.description(), request.occurredAt()));
    }

    @GetMapping
    List<Transfer> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return transfers.list(authorization, page, size);
    }

    @GetMapping("/{id}")
    Transfer get(@RequestHeader(value = "Authorization", required = false) String authorization,
                 @PathVariable UUID id) {
        return transfers.get(authorization, id);
    }

    @PutMapping("/{id}")
    Transfer update(@RequestHeader(value = "Authorization", required = false) String authorization,
                    @PathVariable UUID id, @Valid @RequestBody TransferRequest request) {
        return transfers.update(authorization, id, request.sourceAccountId(), request.destinationAccountId(),
                request.amount(), request.description(), request.occurredAt());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable UUID id) {
        transfers.delete(authorization, id);
        return ResponseEntity.noContent().build();
    }
}
