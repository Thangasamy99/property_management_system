package com.pcc.finance.dto.response;

import com.pcc.finance.entity.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LedgerEntryResponse(
        Long id, String entryRef, String transactionType, String direction,
        BigDecimal amount, LocalDate transactionDate,
        String sourceType, String sourceRef, String paymentRef,
        Long accountId, String accountName, String paymentMethod,
        String postingStatus, Long reversalOfId, String description,
        String createdByName, Instant postedAt
) {
    public static LedgerEntryResponse of(LedgerEntry l) {
        return new LedgerEntryResponse(
                l.getId(), l.getEntryRef(), l.getTransactionType().name(), l.getDirection().name(),
                l.getAmount(), l.getTransactionDate(),
                l.getSourceType(), l.getSourceRef(),
                l.getPayment() != null ? l.getPayment().getPaymentRef() : null,
                l.getAccount() != null ? l.getAccount().getId() : null,
                l.getAccount() != null ? l.getAccount().getName() : null,
                l.getPaymentMethod(), l.getPostingStatus().name(), l.getReversalOfId(),
                l.getDescription(), l.getCreatedByName(), l.getPostedAt());
    }
}
