package com.pcc.finance.dto.response;

import com.pcc.finance.entity.Payment;
import com.pcc.finance.entity.PaymentAllocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PaymentResponse(
        Long id, String paymentRef, String paymentType,
        String payerName, String payerReference,
        BigDecimal amount, BigDecimal refundedAmount, BigDecimal refundableAmount,
        LocalDate paymentDate,
        String methodCode, String methodName,
        String paymentReference, String verificationDetail, String notes,
        Long accountId, String accountName,
        String status, String receiptNo,
        String receivedByName, String postedByName, Instant postedAt, Instant createdAt,
        List<AllocationView> allocations
) {
    public record AllocationView(Long targetId, String targetType, String externalReference,
                                 String parentReference, BigDecimal amount) {
        public static AllocationView of(PaymentAllocation a) {
            var t = a.getTarget();
            return new AllocationView(t.getId(), t.getTargetType().name(), t.getExternalReference(),
                    t.getParentReference(), a.getAmount());
        }
    }

    public static PaymentResponse of(Payment p, String receiptNo, List<PaymentAllocation> allocations) {
        var refundable = p.getAmount().subtract(p.getRefundedAmount());
        return new PaymentResponse(
                p.getId(), p.getPaymentRef(), p.getPaymentType().name(),
                p.getPayerName(), p.getPayerReference(),
                p.getAmount(), p.getRefundedAmount(), refundable,
                p.getPaymentDate(),
                p.getPaymentMethod().getCode(), p.getPaymentMethod().getName(),
                p.getPaymentReference(), p.getVerificationDetail(), p.getNotes(),
                p.getAccount() != null ? p.getAccount().getId() : null,
                p.getAccount() != null ? p.getAccount().getName() : null,
                p.getStatus().name(), receiptNo,
                p.getReceivedByName(), p.getPostedByName(), p.getPostedAt(), p.getCreatedAt(),
                allocations == null ? List.of() : allocations.stream().map(AllocationView::of).toList());
    }
}
