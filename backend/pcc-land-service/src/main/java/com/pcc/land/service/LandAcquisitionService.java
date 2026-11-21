package com.pcc.land.service;

import com.pcc.land.dto.request.AcquisitionRequest;
import com.pcc.land.dto.request.DecisionRequest;
import com.pcc.land.dto.response.AcquisitionResponse;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.entity.LandAcquisition;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.enums.AcquisitionStatus;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.BusinessRuleException;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.LandAcquisitionRepository;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandAcquisitionService {

    private final LandAcquisitionRepository acquisitionRepository;
    private final LandSupport support;

    @Transactional
    public AcquisitionResponse create(AcquisitionRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        UserPrincipal actor = support.actor();
        LandAcquisition a = new LandAcquisition();
        a.setParcel(parcel);
        a.setAcquisitionReference(nextRef());
        a.setSellerName(req.sellerName());
        a.setSellerReference(req.sellerReference());
        a.setSellerContact(req.sellerContact());
        a.setAcquisitionDate(req.acquisitionDate());
        a.setPurchasePrice(req.purchasePrice());
        a.setDocumentReference(req.documentReference());
        a.setStatus(AcquisitionStatus.PENDING);
        if (actor != null) {
            a.setCreatedBy(actor.getId());
            a.setCreatedByName(actor.getUsername());
        }
        a = acquisitionRepository.save(a);
        support.tx(parcel, TransactionType.ACQUISITION_CREATED, a.getId(), a.getAcquisitionReference(),
                a.getPurchasePrice(), a.getStatus().name(), "Acquisition from " + a.getSellerName());
        return AcquisitionResponse.from(a);
    }

    public PageResponse<AcquisitionResponse> list(Long parcelId, AcquisitionStatus status,
                                                  String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.of(acquisitionRepository.search(parcelId, status,
                LandParcelService.blank(search), pageable), AcquisitionResponse::from);
    }

    public AcquisitionResponse get(Long id) {
        return AcquisitionResponse.from(find(id));
    }

    @Transactional
    public AcquisitionResponse approve(Long id, DecisionRequest req) {
        LandAcquisition a = find(id);
        require(a.getStatus() == AcquisitionStatus.PENDING, "Only pending acquisitions can be approved");
        a.setStatus(AcquisitionStatus.APPROVED);
        if (req != null) {
            a.setApprovalReference(req.approvalReference());
            a.setDecisionNote(req.note());
        }
        decide(a);
        a = acquisitionRepository.save(a);
        support.tx(a.getParcel(), TransactionType.ACQUISITION_APPROVED, a.getId(),
                a.getAcquisitionReference(), a.getPurchasePrice(), a.getStatus().name(),
                req != null ? req.note() : null);
        return AcquisitionResponse.from(a);
    }

    @Transactional
    public AcquisitionResponse complete(Long id, DecisionRequest req) {
        LandAcquisition a = find(id);
        require(a.getStatus() == AcquisitionStatus.APPROVED, "Only approved acquisitions can be completed");
        a.setStatus(AcquisitionStatus.COMPLETED);
        if (req != null) {
            a.setDecisionNote(req.note());
        }
        decide(a);
        a = acquisitionRepository.save(a);
        support.tx(a.getParcel(), TransactionType.ACQUISITION_COMPLETED, a.getId(),
                a.getAcquisitionReference(), a.getPurchasePrice(), a.getStatus().name(),
                req != null ? req.note() : null);
        return AcquisitionResponse.from(a);
    }

    @Transactional
    public AcquisitionResponse cancel(Long id, DecisionRequest req) {
        LandAcquisition a = find(id);
        require(a.getStatus() == AcquisitionStatus.PENDING || a.getStatus() == AcquisitionStatus.APPROVED,
                "Only pending or approved acquisitions can be cancelled");
        a.setStatus(AcquisitionStatus.CANCELLED);
        if (req != null) {
            a.setDecisionNote(req.note());
        }
        decide(a);
        a = acquisitionRepository.save(a);
        support.tx(a.getParcel(), TransactionType.ACQUISITION_CANCELLED, a.getId(),
                a.getAcquisitionReference(), a.getPurchasePrice(), a.getStatus().name(),
                req != null ? req.note() : null);
        return AcquisitionResponse.from(a);
    }

    private void decide(LandAcquisition a) {
        UserPrincipal actor = support.actor();
        if (actor != null) {
            a.setDecidedBy(actor.getId());
            a.setDecidedByName(actor.getUsername());
        }
        a.setDecidedAt(Instant.now());
    }

    private LandAcquisition find(Long id) {
        return acquisitionRepository.findById(id)
                .filter(a -> a.getParcel().getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Acquisition not found"));
    }

    /**
     * Finance integration contract: sync money received (Finance Service is
     * the source of truth for payments; this only mirrors the status).
     */
    @Transactional
    public AcquisitionResponse updateFinanceStatus(Long id, java.math.BigDecimal paidAmount) {
        LandAcquisition a = find(id);
        a.setPaidAmount(paidAmount.max(java.math.BigDecimal.ZERO));
        java.math.BigDecimal total = a.getPurchasePrice() == null
                ? java.math.BigDecimal.ZERO : a.getPurchasePrice();
        a.setFinanceStatus(a.getPaidAmount().signum() <= 0 ? "UNPAID"
                : a.getPaidAmount().compareTo(total) >= 0 ? "PAID" : "PARTIALLY_PAID");
        return AcquisitionResponse.from(acquisitionRepository.save(a));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessRuleException(message);
        }
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("ACQ-", acquisitionRepository.maxAcquisitionSequence() + 1);
        } while (acquisitionRepository.existsByAcquisitionReference(ref));
        return ref;
    }
}
