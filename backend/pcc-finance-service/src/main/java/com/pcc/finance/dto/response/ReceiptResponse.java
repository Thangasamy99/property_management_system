package com.pcc.finance.dto.response;

import com.pcc.finance.entity.Receipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReceiptResponse(
        Long id, String receiptNo, Long paymentId, String paymentRef,
        String payerName, BigDecimal amount, String amountInWords,
        LocalDate paymentDate, String paymentMethod, String paymentReference,
        String transactionType, String relatedReference,
        String receivedByName, String organization,
        boolean voided, String issuedByName, Instant issuedAt
) {
    public static ReceiptResponse of(Receipt r) {
        return new ReceiptResponse(
                r.getId(), r.getReceiptNo(), r.getPayment().getId(), r.getPayment().getPaymentRef(),
                r.getPayerName(), r.getAmount(), r.getAmountInWords(),
                r.getPaymentDate(), r.getPaymentMethod(), r.getPaymentReference(),
                r.getTransactionType(), r.getRelatedReference(),
                r.getReceivedByName(), r.getOrganization(),
                r.isVoided(), r.getIssuedByName(), r.getIssuedAt());
    }
}
