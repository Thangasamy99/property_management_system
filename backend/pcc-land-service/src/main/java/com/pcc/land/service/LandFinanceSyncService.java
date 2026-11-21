package com.pcc.land.service;

import com.pcc.land.dto.request.FinanceAllocationRequest;
import com.pcc.land.dto.request.FinanceReversalRequest;
import com.pcc.land.dto.response.AcquisitionResponse;
import com.pcc.land.dto.response.SaleResponse;
import com.pcc.land.entity.LandAcquisition;
import com.pcc.land.entity.LandFinancePayment;
import com.pcc.land.entity.LandFinancePaymentReversal;
import com.pcc.land.entity.LandSale;
import com.pcc.land.exception.BusinessRuleException;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.LandAcquisitionRepository;
import com.pcc.land.repository.LandFinancePaymentRepository;
import com.pcc.land.repository.LandFinancePaymentReversalRepository;
import com.pcc.land.repository.LandSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Finance integration: records Finance-reported payment allocations and
 * reversals against acquisitions/sales. Financial status is always recomputed
 * from the recorded rows — never trusted from the request.
 */
@Service
@RequiredArgsConstructor
public class LandFinanceSyncService {

    private final LandFinancePaymentRepository paymentRepository;
    private final LandFinancePaymentReversalRepository reversalRepository;
    private final LandAcquisitionRepository acquisitionRepository;
    private final LandSaleRepository saleRepository;

    @Transactional
    public AcquisitionResponse recordAcquisitionPayment(Long id, FinanceAllocationRequest req) {
        LandAcquisition a = acquisition(id);
        upsert("ACQUISITION", id, req);
        a.setPaidAmount(paymentRepository.netPaidForTarget("ACQUISITION", id));
        a.setFinanceStatus(financeStatus(a.getPaidAmount(), a.getPurchasePrice()));
        return AcquisitionResponse.from(acquisitionRepository.save(a));
    }

    @Transactional
    public AcquisitionResponse reverseAcquisitionPayment(Long id, FinanceReversalRequest req) {
        LandAcquisition a = acquisition(id);
        applyReversal("ACQUISITION", id, req);
        a.setPaidAmount(paymentRepository.netPaidForTarget("ACQUISITION", id));
        a.setFinanceStatus(financeStatus(a.getPaidAmount(), a.getPurchasePrice()));
        return AcquisitionResponse.from(acquisitionRepository.save(a));
    }

    @Transactional
    public SaleResponse recordSalePayment(Long id, FinanceAllocationRequest req) {
        LandSale s = sale(id);
        upsert("SALE", id, req);
        s.setPaidAmount(paymentRepository.netPaidForTarget("SALE", id));
        s.setFinanceStatus(financeStatus(s.getPaidAmount(), saleTotal(s)));
        return SaleResponse.from(saleRepository.save(s));
    }

    @Transactional
    public SaleResponse reverseSalePayment(Long id, FinanceReversalRequest req) {
        LandSale s = sale(id);
        applyReversal("SALE", id, req);
        s.setPaidAmount(paymentRepository.netPaidForTarget("SALE", id));
        s.setFinanceStatus(financeStatus(s.getPaidAmount(), saleTotal(s)));
        return SaleResponse.from(saleRepository.save(s));
    }

    /** Idempotent on (target, allocationRef): replays are no-ops. */
    private void upsert(String type, Long targetId, FinanceAllocationRequest req) {
        if (paymentRepository
                .findByTargetTypeAndTargetIdAndAllocationRef(type, targetId, req.allocationRef())
                .isPresent()) {
            return;
        }
        LandFinancePayment p = new LandFinancePayment();
        p.setTargetType(type);
        p.setTargetId(targetId);
        p.setPaymentRef(req.paymentRef());
        p.setAllocationRef(req.allocationRef());
        p.setAmount(req.amount());
        paymentRepository.save(p);
    }

    private void applyReversal(String type, Long targetId, FinanceReversalRequest req) {
        if (reversalRepository.existsByReversalRef(req.reversalRef())) {
            return; // idempotent replay
        }
        LandFinancePayment p = paymentRepository
                .findByTargetTypeAndTargetIdAndAllocationRef(type, targetId, req.allocationRef())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment allocation " + req.allocationRef() + " for this record"));
        if (!p.getPaymentRef().equals(req.paymentRef())) {
            throw new BusinessRuleException("Allocation " + req.allocationRef()
                    + " belongs to payment " + p.getPaymentRef() + ", not " + req.paymentRef());
        }
        BigDecimal refundable = p.getAmount().subtract(p.getReversedAmount());
        if (req.amount().compareTo(refundable) > 0) {
            throw new BusinessRuleException("Reversal exceeds refundable allocation amount "
                    + refundable.toPlainString() + " XAF");
        }
        LandFinancePaymentReversal r = new LandFinancePaymentReversal();
        r.setFinancePayment(p);
        r.setReversalRef(req.reversalRef());
        r.setAmount(req.amount());
        r.setReversalDate(req.reversalDate());
        r.setReason(req.reason());
        r.setActor(req.actor());
        reversalRepository.save(r);
        p.setReversedAmount(p.getReversedAmount().add(req.amount()));
        paymentRepository.save(p);
    }

    private LandAcquisition acquisition(Long id) {
        return acquisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acquisition not found"));
    }

    private LandSale sale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
    }

    private static BigDecimal saleTotal(LandSale s) {
        if (s.getAgreedPrice() != null && s.getAgreedPrice().signum() > 0) return s.getAgreedPrice();
        return s.getProposedPrice() == null ? BigDecimal.ZERO : s.getProposedPrice();
    }

    private static String financeStatus(BigDecimal paid, BigDecimal total) {
        if (paid == null || paid.signum() <= 0) return "UNPAID";
        return total != null && paid.compareTo(total) >= 0 ? "PAID" : "PARTIALLY_PAID";
    }
}
