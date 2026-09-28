package com.prumo.transaction.api;

import com.prumo.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999999999999999.99")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 255) String description,
        @NotNull Instant occurredAt,
        UUID categoryId
) {}
