package com.pcc.finance.dto.response;

import com.pcc.finance.entity.OtherIncome;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeResponse(
        Long id, String incomeRef, String category, String description,
        BigDecimal amount, LocalDate incomeDate,
        String methodCode, String methodName,
        String externalReference, String documentReference, String payerName,
        String status, String rejectionReason, String paymentRef,
        String createdByName, String approvedByName, Instant approvedAt, Instant createdAt
) {
    public static IncomeResponse of(OtherIncome i) {
        return new IncomeResponse(
                i.getId(), i.getIncomeRef(), i.getCategory(), i.getDescription(),
                i.getAmount(), i.getIncomeDate(),
                i.getPaymentMethod().getCode(), i.getPaymentMethod().getName(),
                i.getExternalReference(), i.getDocumentReference(), i.getPayerName(),
                i.getStatus().name(), i.getRejectionReason(),
                i.getPayment() != null ? i.getPayment().getPaymentRef() : null,
                i.getCreatedByName(), i.getApprovedByName(), i.getApprovedAt(), i.getCreatedAt());
    }
}
