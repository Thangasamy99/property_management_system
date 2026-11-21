package com.pcc.finance.dto.response;

import com.pcc.finance.entity.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id, String expenseRef, String category, String description,
        BigDecimal amount, LocalDate expenseDate, String department,
        String vendorName, String vendorReference, String documentReference,
        String status, String rejectionReason,
        String methodCode, String paymentReference, LocalDate paidDate, String paymentRef,
        String createdByName, String approvedByName, Instant approvedAt,
        String paidByName, Instant createdAt
) {
    public static ExpenseResponse of(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getExpenseRef(), e.getCategory(), e.getDescription(),
                e.getAmount(), e.getExpenseDate(), e.getDepartment(),
                e.getVendorName(), e.getVendorReference(), e.getDocumentReference(),
                e.getStatus().name(), e.getRejectionReason(),
                e.getPaymentMethod() != null ? e.getPaymentMethod().getCode() : null,
                e.getPaymentReference(), e.getPaidDate(),
                e.getPayment() != null ? e.getPayment().getPaymentRef() : null,
                e.getCreatedByName(), e.getApprovedByName(), e.getApprovedAt(),
                e.getPaidByName(), e.getCreatedAt());
    }
}
