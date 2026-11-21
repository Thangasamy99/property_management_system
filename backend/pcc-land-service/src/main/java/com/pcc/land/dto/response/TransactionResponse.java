package com.pcc.land.dto.response;

import com.pcc.land.entity.LandTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String transactionType,
        Long referenceId,
        String referenceCode,
        BigDecimal amount,
        String status,
        String actorName,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(LandTransaction t) {
        return new TransactionResponse(t.getId(), t.getParcel().getId(), t.getParcel().getParcelReference(),
                t.getParcel().getName(), t.getTransactionType().name(), t.getReferenceId(), t.getReferenceCode(),
                t.getAmount(), t.getStatus(), t.getActorName(), t.getDescription(), t.getCreatedAt());
    }
}
