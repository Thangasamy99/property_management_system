package com.pcc.property.service;

import com.pcc.property.dto.request.ScheduleAllocationRequest;
import com.pcc.property.dto.request.ScheduleReversalRequest;
import com.pcc.property.dto.response.RentScheduleResponse;
import com.pcc.property.entity.RentSchedule;
import com.pcc.property.entity.RentSchedulePayment;
import com.pcc.property.entity.RentSchedulePaymentReversal;
import com.pcc.property.enums.ScheduleStatus;
import com.pcc.property.exception.BusinessRuleException;
import com.pcc.property.exception.DuplicateResourceException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.RentSchedulePaymentRepository;
import com.pcc.property.repository.RentSchedulePaymentReversalRepository;
import com.pcc.property.repository.RentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RentScheduleService {

    private final RentScheduleRepository scheduleRepository;
    private final RentSchedulePaymentRepository paymentRepository;
    private final RentSchedulePaymentReversalRepository reversalRepository;

    /**
     * Finance integration contract: records the total received for a schedule
     * and derives status (PARTIAL / PAID). Only callable with
     * RENT_SCHEDULE_PAYMENT_SYNC. CANCELLED schedules reject sync.
     */
    @Transactional
    public RentScheduleResponse recordPayment(Long id, BigDecimal paidAmount) {
        RentSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rent schedule not found"));
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled schedules cannot receive payments");
        }
        s.setPaidAmount(paidAmount.max(BigDecimal.ZERO));
        deriveStatus(s);
        return RentScheduleResponse.of(scheduleRepository.save(s));
    }

    /**
     * Finance integration contract: records one payment allocation against a
     * schedule. Idempotent on (schedule, allocationRef) — a replayed sync returns
     * the current schedule without duplicating the record.
     */
    @Transactional
    public RentScheduleResponse recordAllocation(Long id, ScheduleAllocationRequest req) {
        RentSchedule s = locked(id);
        return paymentRepository.findByScheduleIdAndAllocationRef(id, req.allocationRef())
                .map(existing -> RentScheduleResponse.of(s))
                .orElseGet(() -> {
                    RentSchedulePayment p = new RentSchedulePayment();
                    p.setSchedule(s);
                    p.setPaymentRef(req.paymentRef());
                    p.setAllocationRef(req.allocationRef());
                    p.setAmount(req.amount());
                    paymentRepository.save(p);
                    recompute(s);
                    return RentScheduleResponse.of(scheduleRepository.save(s));
                });
    }

    /**
     * Finance integration contract: reverses part or all of a recorded
     * allocation. Idempotent on reversalRef; validates against the allocation's
     * refundable remainder. Original payment record is preserved — only the
     * reversed amount increases.
     */
    @Transactional
    public RentScheduleResponse recordReversal(Long id, ScheduleReversalRequest req) {
        RentSchedule s = locked(id);
        if (reversalRepository.existsByReversalRef(req.reversalRef())) {
            return RentScheduleResponse.of(s); // idempotent replay
        }
        RentSchedulePayment p = paymentRepository
                .findByScheduleIdAndAllocationRef(id, req.allocationRef())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment allocation " + req.allocationRef() + " for this schedule"));
        if (!p.getPaymentRef().equals(req.paymentRef())) {
            throw new BusinessRuleException("Allocation " + req.allocationRef()
                    + " belongs to payment " + p.getPaymentRef() + ", not " + req.paymentRef());
        }
        BigDecimal refundable = p.getAmount().subtract(p.getReversedAmount());
        if (req.amount().compareTo(refundable) > 0) {
            throw new BusinessRuleException("Reversal exceeds refundable allocation amount "
                    + refundable.toPlainString() + " XAF");
        }

        RentSchedulePaymentReversal r = new RentSchedulePaymentReversal();
        r.setSchedule(s);
        r.setSchedulePayment(p);
        r.setReversalRef(req.reversalRef());
        r.setAmount(req.amount());
        r.setReversalDate(req.reversalDate());
        r.setReason(req.reason());
        r.setActor(req.actor());
        reversalRepository.save(r);

        p.setReversedAmount(p.getReversedAmount().add(req.amount()));
        paymentRepository.save(p);

        recompute(s);
        return RentScheduleResponse.of(scheduleRepository.save(s));
    }

    private RentSchedule locked(Long id) {
        RentSchedule s = scheduleRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rent schedule not found"));
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled schedules cannot receive payment events");
        }
        return s;
    }

    /** paid = SUM(allocation.amount - allocation.reversedAmount). */
    private void recompute(RentSchedule s) {
        s.setPaidAmount(paymentRepository.netPaidForSchedule(s.getId()));
        deriveStatus(s);
    }

    private void deriveStatus(RentSchedule s) {
        if (s.getPaidAmount().compareTo(s.getAmount()) >= 0) {
            s.setStatus(ScheduleStatus.PAID);
        } else if (s.getPaidAmount().signum() > 0) {
            s.setStatus(ScheduleStatus.PARTIAL);
        } else {
            s.setStatus(s.getDueDate().isBefore(LocalDate.now())
                    ? ScheduleStatus.OVERDUE : ScheduleStatus.PENDING);
        }
    }
}
