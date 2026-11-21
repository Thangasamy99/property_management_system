package com.pcc.land.service;

import com.pcc.land.dto.request.SaleDecisionRequest;
import com.pcc.land.dto.request.SaleRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.SaleResponse;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.entity.LandSale;
import com.pcc.land.enums.LandStatus;
import com.pcc.land.enums.SaleStatus;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.BusinessRuleException;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.LandSaleRepository;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandSaleService {

    /** Non-terminal sale states that block a new sale on the same parcel. */
    private static final Set<SaleStatus> BLOCKING = EnumSet.of(
            SaleStatus.DRAFT, SaleStatus.SUBMITTED, SaleStatus.UNDER_REVIEW,
            SaleStatus.APPROVED, SaleStatus.PAYMENT_PENDING, SaleStatus.LEGAL_PROCESSING);

    private final LandSaleRepository saleRepository;
    private final LandSupport support;

    @Transactional
    public SaleResponse create(SaleRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        if (saleRepository.existsByParcelIdAndStatusIn(parcel.getId(), BLOCKING)) {
            throw new BusinessRuleException("An active sale already exists for this parcel");
        }
        UserPrincipal actor = support.actor();
        LandSale s = new LandSale();
        s.setParcel(parcel);
        s.setSaleReference(nextRef());
        s.setBuyerName(req.buyerName());
        s.setBuyerReference(req.buyerReference());
        s.setBuyerContact(req.buyerContact());
        s.setProposedPrice(req.proposedPrice());
        s.setAgreementReference(req.agreementReference());
        s.setAgreementDate(req.agreementDate());
        s.setDocumentReference(req.documentReference());
        s.setStatus(SaleStatus.DRAFT);
        if (actor != null) {
            s.setCreatedBy(actor.getId());
            s.setCreatedByName(actor.getUsername());
        }
        s = saleRepository.save(s);
        support.tx(parcel, TransactionType.SALE_CREATED, s.getId(), s.getSaleReference(),
                s.getProposedPrice(), s.getStatus().name(), "Sale request for " + s.getBuyerName());
        return SaleResponse.from(s);
    }

    public PageResponse<SaleResponse> list(Long parcelId, SaleStatus status,
                                           String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.of(saleRepository.search(parcelId, status,
                LandParcelService.blank(search), pageable), SaleResponse::from);
    }

    public SaleResponse get(Long id) {
        return SaleResponse.from(find(id));
    }

    @Transactional
    public SaleResponse submit(Long id) {
        LandSale s = find(id);
        require(s.getStatus() == SaleStatus.DRAFT, "Only draft sales can be submitted");
        s.setStatus(SaleStatus.SUBMITTED);
        s.setSubmittedAt(Instant.now());
        s = saleRepository.save(s);
        support.setParcelStatus(s.getParcel(), LandStatus.UNDER_SALE, "Sale " + s.getSaleReference() + " submitted");
        support.tx(s.getParcel(), TransactionType.SALE_SUBMITTED, s.getId(), s.getSaleReference(),
                s.getProposedPrice(), s.getStatus().name(), null);
        return SaleResponse.from(s);
    }

    @Transactional
    public SaleResponse startReview(Long id) {
        LandSale s = find(id);
        require(s.getStatus() == SaleStatus.SUBMITTED, "Only submitted sales can enter review");
        s.setStatus(SaleStatus.UNDER_REVIEW);
        s = saleRepository.save(s);
        support.tx(s.getParcel(), TransactionType.SALE_PROGRESSED, s.getId(), s.getSaleReference(),
                null, s.getStatus().name(), "Sale under review");
        return SaleResponse.from(s);
    }

    @Transactional
    public SaleResponse approve(Long id, SaleDecisionRequest req) {
        LandSale s = find(id);
        require(s.getStatus() == SaleStatus.SUBMITTED || s.getStatus() == SaleStatus.UNDER_REVIEW,
                "Only submitted or under-review sales can be approved");
        s.setStatus(SaleStatus.APPROVED);
        if (req != null) {
            s.setAgreedPrice(req.agreedPrice());
            s.setDecisionNote(req.note());
        }
        decide(s);
        s = saleRepository.save(s);
        support.tx(s.getParcel(), TransactionType.SALE_APPROVED, s.getId(), s.getSaleReference(),
                s.getAgreedPrice() != null ? s.getAgreedPrice() : s.getProposedPrice(),
                s.getStatus().name(), req != null ? req.note() : null);
        return SaleResponse.from(s);
    }

    @Transactional
    public SaleResponse reject(Long id, SaleDecisionRequest req) {
        LandSale s = find(id);
        require(s.getStatus() == SaleStatus.SUBMITTED || s.getStatus() == SaleStatus.UNDER_REVIEW,
                "Only submitted or under-review sales can be rejected");
        s.setStatus(SaleStatus.REJECTED);
        if (req != null) {
            s.setDecisionNote(req.note());
        }
        decide(s);
        s = saleRepository.save(s);
        releaseParcel(s, "Sale " + s.getSaleReference() + " rejected");
        support.tx(s.getParcel(), TransactionType.SALE_REJECTED, s.getId(), s.getSaleReference(),
                null, s.getStatus().name(), req != null ? req.note() : null);
        return SaleResponse.from(s);
    }

    /** APPROVED → PAYMENT_PENDING → LEGAL_PROCESSING → COMPLETED. */
    @Transactional
    public SaleResponse progress(Long id) {
        LandSale s = find(id);
        SaleStatus next = switch (s.getStatus()) {
            case APPROVED -> SaleStatus.PAYMENT_PENDING;
            case PAYMENT_PENDING -> SaleStatus.LEGAL_PROCESSING;
            case LEGAL_PROCESSING -> SaleStatus.COMPLETED;
            default -> throw new BusinessRuleException(
                    "Sale cannot be progressed from status " + s.getStatus());
        };
        s.setStatus(next);
        s = saleRepository.save(s);
        if (next == SaleStatus.COMPLETED) {
            support.setParcelStatus(s.getParcel(), LandStatus.SOLD, "Sale " + s.getSaleReference() + " completed");
        }
        support.tx(s.getParcel(),
                next == SaleStatus.COMPLETED ? TransactionType.SALE_COMPLETED : TransactionType.SALE_PROGRESSED,
                s.getId(), s.getSaleReference(), s.getAgreedPrice(), next.name(), null);
        return SaleResponse.from(s);
    }

    @Transactional
    public SaleResponse cancel(Long id, SaleDecisionRequest req) {
        LandSale s = find(id);
        require(s.getStatus() == SaleStatus.DRAFT || s.getStatus() == SaleStatus.SUBMITTED
                        || s.getStatus() == SaleStatus.UNDER_REVIEW || s.getStatus() == SaleStatus.APPROVED
                        || s.getStatus() == SaleStatus.PAYMENT_PENDING,
                "Sale cannot be cancelled from status " + s.getStatus());
        s.setStatus(SaleStatus.CANCELLED);
        if (req != null) {
            s.setDecisionNote(req.note());
        }
        decide(s);
        s = saleRepository.save(s);
        releaseParcel(s, "Sale " + s.getSaleReference() + " cancelled");
        support.tx(s.getParcel(), TransactionType.SALE_CANCELLED, s.getId(), s.getSaleReference(),
                null, s.getStatus().name(), req != null ? req.note() : null);
        return SaleResponse.from(s);
    }

    private void releaseParcel(LandSale s, String reason) {
        if (s.getParcel().getStatus() == LandStatus.UNDER_SALE) {
            support.setParcelStatus(s.getParcel(), LandStatus.AVAILABLE, reason);
        }
    }

    private void decide(LandSale s) {
        UserPrincipal actor = support.actor();
        if (actor != null) {
            s.setDecidedBy(actor.getId());
            s.setDecidedByName(actor.getUsername());
        }
        s.setDecidedAt(Instant.now());
    }

    private LandSale find(Long id) {
        return saleRepository.findById(id)
                .filter(s -> s.getParcel().getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessRuleException(message);
        }
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("SAL-", saleRepository.maxSaleSequence() + 1);
        } while (saleRepository.existsBySaleReference(ref));
        return ref;
    }
}
