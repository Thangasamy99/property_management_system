package com.pcc.finance.service;

import com.pcc.finance.dto.request.PaymentRequest;
import com.pcc.finance.dto.request.PostPaymentRequest;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.PaymentResponse;
import com.pcc.finance.dto.response.TargetResponse;
import com.pcc.finance.entity.*;
import com.pcc.finance.enums.*;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.integration.TargetVerifier;
import com.pcc.finance.integration.VerifiedTarget;
import com.pcc.finance.repository.*;
import com.pcc.finance.util.NumberToWords;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTargetRepository targetRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final ReceiptRepository receiptRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final TargetVerifier verifier;
    private final PostingService posting;
    private final FinanceSupport support;

    // ---------- creation ----------

    /** Verify + record a collection payment against an external target. */
    @Transactional
    public PaymentResponse create(PaymentRequest req, PaymentType type) {
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(req.idempotencyKey().trim());
            if (existing.isPresent()) return response(existing.get());
        }

        VerifiedTarget verified = verify(req, type);
        PaymentTarget target = lockTarget(type, verified);

        BigDecimal committed = committedFor(target.getId());
        BigDecimal outstanding = target.getTotalAmount().subtract(committed);
        if (req.amount().compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Amount " + req.amount().toPlainString()
                    + " exceeds outstanding " + outstanding.toPlainString() + " for " + target.getExternalReference());
        }

        Payment p = new Payment();
        p.setPaymentType(type);
        p.setPayerName(req.payerName());
        p.setPayerReference(req.payerReference());
        p.setAmount(req.amount());
        p.setPaymentDate(req.paymentDate());
        p.setPaymentMethod(support.method(req.methodCode()));
        p.setPaymentReference(req.paymentReference());
        p.setVerificationDetail(req.verificationDetail());
        p.setNotes(req.notes());
        p.setIdempotencyKey(req.idempotencyKey() == null || req.idempotencyKey().isBlank()
                ? null : req.idempotencyKey().trim());
        p.setReceivedById(support.actorId());
        p.setReceivedByName(support.actorName());

        boolean cashLike = "CASH".equalsIgnoreCase(req.methodCode());
        if (cashLike) {
            p.setAccount(support.account(req.accountId()));
            markPosted(p);
        } else {
            p.setStatus(PaymentStatus.RECORDED);
        }
        p.setPaymentRef(FinanceSupport.ref("PAY-", paymentRepository.maxPaymentSequence() + 1));
        p = paymentRepository.save(p);

        PaymentAllocation alloc = new PaymentAllocation();
        alloc.setPayment(p);
        alloc.setTarget(target);
        alloc.setAmount(req.amount());
        alloc.setNote(verified.description());
        allocationRepository.save(alloc);

        if (p.getStatus() == PaymentStatus.POSTED) {
            postEffects(p, target);
        }
        support.audit("PAYMENT", p.getId(), "CREATED",
                type + " " + p.getAmount().toPlainString() + " XAF -> " + target.getExternalReference());
        return response(p);
    }

    /** Post a previously RECORDED (non-cash) payment once verification evidence exists. */
    @Transactional
    public PaymentResponse post(Long id, PostPaymentRequest req) {
        Payment p = payment(id);
        if (p.getStatus() != PaymentStatus.RECORDED) {
            throw new BusinessRuleException("Only recorded payments can be posted (current: "
                    + p.getStatus().name().toLowerCase(Locale.ROOT) + ")");
        }
        if (req.verificationDetail() != null) p.setVerificationDetail(req.verificationDetail());
        if (req.accountId() != null) p.setAccount(support.account(req.accountId()));

        List<PaymentAllocation> allocs = allocationRepository.findByPaymentId(p.getId());
        for (PaymentAllocation a : allocs) {
            PaymentTarget t = targetRepository.findForUpdate(a.getTarget().getTargetType(),
                    a.getTarget().getExternalId()).orElseThrow();
            BigDecimal committed = postedNetFor(t.getId());
            BigDecimal outstanding = t.getTotalAmount().subtract(committed);
            if (a.getAmount().compareTo(outstanding) > 0) {
                throw new BusinessRuleException("Posting would overpay " + t.getExternalReference()
                        + " (outstanding " + outstanding.toPlainString() + " XAF)");
            }
        }

        markPosted(p);
        paymentRepository.save(p);
        for (PaymentAllocation a : allocs) postEffects(p, a.getTarget());
        support.audit("PAYMENT", p.getId(), "POSTED", p.getPaymentRef());
        return response(p);
    }

    private void markPosted(Payment p) {
        p.setStatus(PaymentStatus.POSTED);
        p.setPostedById(support.actorId());
        p.setPostedByName(support.actorName());
        p.setPostedAt(Instant.now());
    }

    /** Ledger CREDIT + receipt + downstream status sync for a posted payment. */
    private void postEffects(Payment p, PaymentTarget target) {
        String related = target.getExternalReference()
                + (target.getParentReference() != null ? " / " + target.getParentReference() : "");
        posting.credit(p, LedgerTxType.RECEIPT, related,
                p.getPaymentType() + " payment");
        posting.issueReceipt(p, related);

        BigDecimal net = postedNetFor(target.getId());
        switch (target.getTargetType()) {
            case RENT_SCHEDULE -> verifier.syncRentSchedule(target.getExternalId(), net);
            case LAND_ACQUISITION -> verifier.syncLandAcquisition(target.getExternalId(), net);
            case LAND_SALE -> verifier.syncLandSale(target.getExternalId(), net);
        }
    }

    // ---------- verification helpers ----------

    private VerifiedTarget verify(PaymentRequest req, PaymentType type) {
        return switch (type) {
            case RENT -> {
                if (req.parentId() == null) {
                    throw new BusinessRuleException("parentId (rental agreement id) is required for rent payments");
                }
                yield verifier.rentSchedule(req.parentId(), req.externalId());
            }
            case LAND_ACQUISITION -> verifier.landAcquisition(req.externalId());
            case LAND_SALE -> verifier.landSale(req.externalId());
            default -> throw new BusinessRuleException("Unsupported collection type " + type);
        };
    }

    /** Fetch the target row under pessimistic lock; create it (with snapshot) if first payment. */
    private PaymentTarget lockTarget(PaymentType type, VerifiedTarget v) {
        TargetType tt = switch (type) {
            case RENT -> TargetType.RENT_SCHEDULE;
            case LAND_ACQUISITION -> TargetType.LAND_ACQUISITION;
            case LAND_SALE -> TargetType.LAND_SALE;
            default -> throw new BusinessRuleException("No target type for " + type);
        };
        return targetRepository.findForUpdate(tt, v.externalId()).orElseGet(() -> {
            PaymentTarget t = new PaymentTarget();
            t.setTargetType(tt);
            t.setExternalId(v.externalId());
            t.setExternalReference(v.externalReference());
            t.setParentReference(v.parentReference());
            t.setDescription(v.description());
            t.setTotalAmount(v.totalAmount());
            t.setSnapshot(v.snapshotJson());
            return targetRepository.save(t);
        });
    }

    /** Committed funds: allocations on payments that still occupy the balance (not reversed/void). */
    private BigDecimal committedFor(Long targetId) {
        return allocationRepository.sumCommittedForTarget(targetId);
    }

    /** Net received (posted, less refunds) — used for outstanding + downstream sync. */
    private BigDecimal postedNetFor(Long targetId) {
        BigDecimal posted = allocationRepository.sumPostedForTarget(targetId);
        BigDecimal refunded = allocationRepository.sumRefundedForTarget(targetId);
        return posted.subtract(refunded);
    }

    // ---------- reads ----------

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(PaymentType type, PaymentStatus status,
                                              LocalDate from, LocalDate to, String q,
                                              int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                paymentRepository.search(type, status, from, to, like, PageRequest.of(page, size)),
                this::response);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return response(payment(id));
    }

    @Transactional(readOnly = true)
    public TargetResponse targetStatus(TargetType type, Long externalId) {
        PaymentTarget t = targetRepository.findByTargetTypeAndExternalId(type, externalId)
                .orElseThrow(() -> new ResourceNotFoundException("No payments recorded for this target"));
        return targetView(t);
    }

    @Transactional(readOnly = true)
    public List<TargetResponse> openTargets(TargetType type) {
        return targetRepository.findAll().stream()
                .filter(t -> t.getTargetType() == type)
                .map(this::targetView)
                .filter(v -> !v.fullyPaid())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentAllocation> allocationsFor(Long targetId) {
        return allocationRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
    }

    public TargetResponse targetView(PaymentTarget t) {
        BigDecimal net = postedNetFor(t.getId());
        BigDecimal outstanding = t.getTotalAmount().subtract(net);
        if (outstanding.signum() < 0) outstanding = BigDecimal.ZERO;
        return new TargetResponse(t.getId(), t.getTargetType().name(), t.getExternalId(),
                t.getExternalReference(), t.getParentReference(), t.getDescription(),
                t.getTotalAmount(), net, outstanding, outstanding.signum() == 0);
    }

    Payment payment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private PaymentResponse response(Payment p) {
        String receiptNo = receiptRepository.findByPaymentId(p.getId())
                .map(Receipt::getReceiptNo).orElse(null);
        return PaymentResponse.of(p, receiptNo, allocationRepository.findByPaymentId(p.getId()));
    }
}
