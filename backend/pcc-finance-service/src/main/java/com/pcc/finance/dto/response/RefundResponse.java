package com.pcc.finance.dto.response;

import com.pcc.finance.entity.Refund;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long id, String refundRef, Long paymentId, String paymentRef,
        String payerName, BigDecimal amount, String reason, String status,
        String requestedByName, Instant requestedAt,
        String decidedByName, Instant decidedAt, String decisionNote
) {
    public static RefundResponse of(Refund r) {
        return new RefundResponse(
                r.getId(), r.getRefundRef(), r.getPayment().getId(), r.getPayment().getPaymentRef(),
                r.getPayment().getPayerName(), r.getAmount(), r.getReason(), r.getStatus().name(),
                r.getRequestedByName(), r.getRequestedAt(),
                r.getDecidedByName(), r.getDecidedAt(), r.getDecisionNote());
    }
}
