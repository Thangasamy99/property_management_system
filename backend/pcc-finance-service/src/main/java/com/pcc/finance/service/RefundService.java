package com.pcc.finance.service;

import com.pcc.finance.dto.request.RefundRequest;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.RefundResponse;
import com.pcc.finance.entity.LedgerEntry;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.entity.PaymentAllocation;
import com.pcc.finance.entity.Refund;
import com.pcc.finance.enums.*;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.LedgerEntryRepository;
import com.pcc.finance.repository.PaymentAllocationRepository;
import com.pcc.finance.repository.PaymentRepository;
import com.pcc.finance.repository.ReceiptRepository;
import com.pcc.finance.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final ReceiptRepository receiptRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final PostingService posting;
    private final FinanceSupport support;
    private final SyncService sync;

    // ---------- refunds ----------

    @Transactional
    public RefundResponse request(RefundRequest req) {
        Payment p = payment(req.paymentId());
        if (p.getStatus() != PaymentStatus.POSTED && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("Only posted payments can be refunded");
        }
        BigDecimal refundable = p.getAmount().subtract(p.getRefundedAmount());
        if (req.amount().compareTo(refundable) > 0) {
            throw new BusinessRuleException("Refund exceeds refundable amount " + refundable.toPlainString());
        }
        Refund r = new Refund();
        r.setPayment(p);
        r.setAmount(req.amount());
        r.setReason(req.reason());
        r.setRequestedById(support.actorId());
        r.setRequestedByName(support.actorName());
        r.setRefundRef(FinanceSupport.ref("RFD-", refundRepository.maxRefundSequence() + 1));
        r = refundRepository.save(r);
        support.audit("REFUND", r.getId(), "REQUESTED", p.getPaymentRef() + " " + req.amount());
        return RefundResponse.of(r);
    }

    @Transactional
    public RefundResponse approve(Long id) {
        Refund r = refund(id);
        if (r.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessRuleException("Only requested refunds can be approved");
        }
        Payment p = r.getPayment();
        BigDecimal refundable = p.getAmount().subtract(p.getRefundedAmount());
        if (r.getAmount().compareTo(refundable) > 0) {
            throw new BusinessRuleException("Refundable amount changed; refund can no longer be posted");
        }
        p.setRefundedAmount(p.getRefundedAmount().add(r.getAmount()));
        p.setStatus(p.getRefundedAmount().compareTo(p.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(p);

        distributeReversal(p, r.getAmount(), r.getRefundRef(), r.getReason());

        posting.debit(p, LedgerTxType.REFUND, r.getAmount(), r.getRefundRef(),
                "Refund of " + p.getPaymentRef() + ": " + r.getReason());

        r.setStatus(RefundStatus.POSTED);
        r.setDecidedById(support.actorId());
        r.setDecidedByName(support.actorName());
        r.setDecidedAt(Instant.now());
        refundRepository.save(r);
        support.audit("REFUND", r.getId(), "POSTED", p.getPaymentRef());
        return RefundResponse.of(r);
    }

    @Transactional
    public RefundResponse reject(Long id, String note) {
        Refund r = refund(id);
        if (r.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessRuleException("Only requested refunds can be rejected");
        }
        r.setStatus(RefundStatus.REJECTED);
        r.setDecisionNote(note);
        r.setDecidedById(support.actorId());
        r.setDecidedByName(support.actorName());
        r.setDecidedAt(Instant.now());
        refundRepository.save(r);
        return RefundResponse.of(r);
    }

    // ---------- reversals ----------

    /** Reverse a POSTED payment entirely: reversing ledger entries, receipt voided, original preserved. */
    @Transactional
    public Payment reverse(Long paymentId, String reason) {
        Payment p = payment(paymentId);
        if (p.getStatus() != PaymentStatus.POSTED) {
            throw new BusinessRuleException(
                    "Only fully posted, unrefunded payments can be reversed (status: " + p.getStatus() + ")");
        }
        List<LedgerEntry> entries = ledgerRepository.findByPaymentId(p.getId()).stream()
                .filter(e -> e.getPostingStatus() == PostingStatus.POSTED)
                .toList();
        for (LedgerEntry e : entries) posting.reversal(e, reason);

        p.setStatus(PaymentStatus.REVERSED);
        paymentRepository.save(p);

        List<PaymentAllocation> allocs = allocationRepository.findByPaymentId(p.getId());
        int seq = 0;
        for (PaymentAllocation a : allocs) {
            seq++;
            BigDecimal portion = a.getAmount().subtract(a.getReversedAmount());
            if (portion.signum() <= 0) continue;
            a.setReversedAmount(a.getAmount());
            allocationRepository.save(a);
            sync.enqueueReversal(a.getTarget(), p.getPaymentRef(), a,
                    "REV-" + p.getPaymentRef() + "-" + seq, portion,
                    java.time.LocalDate.now(), reason, support.actorName());
        }

        receiptRepository.findByPaymentId(p.getId()).ifPresent(r -> {
            r.setVoided(true);
            receiptRepository.save(r);
        });
        support.audit("PAYMENT", p.getId(), "REVERSED", reason);
        return p;
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> list(RefundStatus status, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                refundRepository.search(status, like, PageRequest.of(page, size)), RefundResponse::of);
    }

    @Transactional(readOnly = true)
    public RefundResponse get(Long id) {
        return RefundResponse.of(refund(id));
    }

    /**
     * Distributes a refund across the payment's allocations (FIFO, in id order),
     * records the reversed portion per allocation and enqueues a downstream
     * reversal sync per affected allocation — all inside this transaction.
     */
    private void distributeReversal(Payment p, BigDecimal amount, String refundRef, String reason) {
        BigDecimal remaining = amount;
        int seq = 0;
        for (PaymentAllocation a : allocationRepository.findByPaymentId(p.getId())) {
            if (remaining.signum() <= 0) break;
            seq++;
            BigDecimal refundable = a.getAmount().subtract(a.getReversedAmount());
            BigDecimal portion = refundable.min(remaining);
            if (portion.signum() <= 0) continue;
            a.setReversedAmount(a.getReversedAmount().add(portion));
            allocationRepository.save(a);
            sync.enqueueReversal(a.getTarget(), p.getPaymentRef(), a,
                    refundRef + "-" + seq, portion,
                    java.time.LocalDate.now(), reason, support.actorName());
            remaining = remaining.subtract(portion);
        }
        if (remaining.signum() > 0) {
            throw new BusinessRuleException("Refund exceeds recorded allocation balances");
        }
    }

    private Payment payment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private Refund refund(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
    }
}
