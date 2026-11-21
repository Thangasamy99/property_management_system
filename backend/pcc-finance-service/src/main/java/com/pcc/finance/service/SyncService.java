package com.pcc.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.finance.entity.PaymentAllocation;
import com.pcc.finance.entity.PaymentTarget;
import com.pcc.finance.entity.SyncOutbox;
import com.pcc.finance.enums.TargetType;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.integration.ServiceGateway;
import com.pcc.finance.repository.PaymentAllocationRepository;
import com.pcc.finance.repository.SyncOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent downstream-sync outbox. Rows are created inside the caller's
 * transaction so sync work can never be lost after a posting commit; dispatch
 * marks rows SUCCESS only on a confirmed 2xx. Failures stay PENDING/FAILED and
 * are retried by {@link #retryPending()} or an authorized manual retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final int MAX_AUTO_RETRIES = 10;

    private final SyncOutboxRepository outboxRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final ServiceGateway gateway;
    private final FinanceSupport support;
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- enqueue (called inside the posting transaction) ----------

    /** Enqueue + attempt a payment-allocation sync for one allocation. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueuePayment(PaymentTarget target, String paymentRef, PaymentAllocation alloc) {
        Map<String, Object> payload = Map.of(
                "paymentRef", paymentRef,
                "allocationRef", alloc.getAllocationRef(),
                "amount", alloc.getAmount().subtract(alloc.getReversedAmount()));
        enqueueAndDispatch(alloc.getAllocationRef(), SyncOutbox.Kind.PAYMENT, target, payload);
    }

    /**
     * Enqueue + attempt one reversal sync. {@code reversalRef} must be stable
     * across retries — the downstream service deduplicates on it.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueueReversal(PaymentTarget target, String paymentRef, PaymentAllocation alloc,
                                String reversalRef, BigDecimal amount, LocalDate date,
                                String reason, String actor) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentRef", paymentRef);
        payload.put("allocationRef", alloc.getAllocationRef());
        payload.put("reversalRef", reversalRef);
        payload.put("amount", amount);
        payload.put("reversalDate", date == null ? null : date.toString());
        payload.put("reason", reason);
        payload.put("actor", actor);
        enqueueAndDispatch(reversalRef, SyncOutbox.Kind.REVERSAL, target, payload);
    }

    private void enqueueAndDispatch(String syncRef, SyncOutbox.Kind kind, PaymentTarget target,
                                    Map<String, Object> payload) {
        SyncOutbox row = outboxRepository.findBySyncRef(syncRef).orElseGet(() -> {
            SyncOutbox o = new SyncOutbox();
            o.setSyncRef(syncRef);
            o.setSyncKind(kind);
            o.setExternalService(target.getTargetType() == TargetType.RENT_SCHEDULE ? "PROPERTY" : "LAND");
            o.setTargetType(target.getTargetType().name());
            o.setTargetId(target.getExternalId());
            o.setPayload(write(payload));
            return outboxRepository.save(o);
        });
        if (row.getStatus() == SyncOutbox.Status.SUCCESS) return; // already confirmed
        dispatch(row);
    }

    // ---------- dispatch ----------

    private void dispatch(SyncOutbox row) {
        if (!gateway.hasRelayToken()) {
            // No user JWT to relay — leave PENDING; a request-driven dispatch
            // (posting call or authorized manual retry) will deliver it.
            return;
        }
        row.setRetryCount(row.getRetryCount() + 1);
        row.setLastAttemptAt(Instant.now());
        try {
            Map<String, Object> payload = mapper.readValue(row.getPayload(), Map.class);
            String path = switch (row.getSyncKind()) {
                case PAYMENT -> switch (TargetType.valueOf(row.getTargetType())) {
                    case RENT_SCHEDULE -> "/api/v1/rent-schedules/" + row.getTargetId() + "/payments";
                    case LAND_ACQUISITION -> "/api/v1/land-acquisitions/" + row.getTargetId() + "/finance-payments";
                    case LAND_SALE -> "/api/v1/land-sales/" + row.getTargetId() + "/finance-payments";
                };
                case REVERSAL -> switch (TargetType.valueOf(row.getTargetType())) {
                    case RENT_SCHEDULE -> "/api/v1/rent-schedules/" + row.getTargetId() + "/payment-reversals";
                    case LAND_ACQUISITION -> "/api/v1/land-acquisitions/" + row.getTargetId() + "/payment-reversals";
                    case LAND_SALE -> "/api/v1/land-sales/" + row.getTargetId() + "/payment-reversals";
                };
            };
            ServiceGateway.SyncCallResult result = "PROPERTY".equals(row.getExternalService())
                    ? gateway.propertyPost(path, payload)
                    : gateway.landPost(path, payload);
            row.setStatus(result.success() ? SyncOutbox.Status.SUCCESS : SyncOutbox.Status.FAILED);
            row.setPermanent(result.permanent());
            row.setLastError(result.error());
        } catch (Exception e) {
            row.setStatus(SyncOutbox.Status.FAILED);
            row.setLastError("Dispatch error: " + e.getMessage());
        }
        row.setUpdatedAt(Instant.now());
        outboxRepository.save(row);
    }

    /** Scheduled retry of non-permanent failures. */
    @Scheduled(fixedDelayString = "${pcc.sync.retry-delay-ms:60000}")
    @Transactional
    public void retryPending() {
        List<SyncOutbox> retryable = outboxRepository.findRetryable(MAX_AUTO_RETRIES);
        if (!retryable.isEmpty()) {
            log.info("Retrying {} pending sync item(s)", retryable.size());
            retryable.forEach(this::dispatch);
        }
    }

    // ---------- support / audit surface ----------

    @Transactional(readOnly = true)
    public List<SyncOutbox> listFor(String paymentOrRefundRef) {
        return outboxRepository.findRelatedTo(paymentOrRefundRef);
    }

    @Transactional(readOnly = true)
    public List<SyncOutbox> listAll() {
        return outboxRepository.findAll();
    }

    /** Manual retry — authorized finance staff only (controller enforces). */
    @Transactional
    public SyncOutbox retry(Long id) {
        SyncOutbox row = outboxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sync record not found"));
        if (row.getStatus() == SyncOutbox.Status.SUCCESS) {
            throw new BusinessRuleException("Sync already confirmed successful");
        }
        dispatch(row);
        support.audit("SYNC", row.getId(), "MANUAL_RETRY", row.getSyncRef() + " -> " + row.getStatus());
        return row;
    }

    /**
     * Reconciliation: enqueues a PAYMENT sync for every posted allocation that
     * has no outbox record yet — repairs targets that were previously synced via
     * the legacy absolute-total PATCH before allocation-level sync existed.
     * Runs at startup and is exposed to authorized users; fully idempotent.
     */
    @org.springframework.context.event.EventListener(
            org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void reconcileOnStartup() {
        int enqueued = reconcile();
        if (enqueued > 0) log.info("Reconciled {} missing downstream payment sync(s)", enqueued);
    }

    @Transactional
    public int reconcile() {
        int count = 0;
        for (PaymentAllocation a : allocationRepository.findPostedAllocations()) {
            if (outboxRepository.findBySyncRef(a.getAllocationRef()).isPresent()) continue;
            enqueuePayment(a.getTarget(), a.getPayment().getPaymentRef(), a);
            count++;
        }
        return count;
    }

    private String write(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize sync payload", e);
        }
    }
}
