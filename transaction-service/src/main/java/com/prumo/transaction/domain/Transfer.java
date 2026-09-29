package com.prumo.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record Transfer(UUID id, UUID ownerId, UUID sourceAccountId, UUID destinationAccountId,
                       @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount, String description, Instant occurredAt, Instant createdAt) {
}
