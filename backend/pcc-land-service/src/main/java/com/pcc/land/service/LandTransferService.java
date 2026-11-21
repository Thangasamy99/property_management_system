package com.pcc.land.service;

import com.pcc.land.dto.request.TransferCompleteRequest;
import com.pcc.land.dto.request.TransferRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.TransferResponse;
import com.pcc.land.entity.*;
import com.pcc.land.enums.*;
import com.pcc.land.exception.BusinessRuleException;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.*;
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
public class LandTransferService {

    private static final Set<TransferStatus> BLOCKING = EnumSet.of(
            TransferStatus.DRAFT, TransferStatus.SUBMITTED, TransferStatus.APPROVED);

    private final LandTransferRepository transferRepository;
    private final LandOwnershipRepository ownershipRepository;
    private final LandSaleRepository saleRepository;
    private final LandOwnershipService ownershipService;
    private final LandSupport support;

    @Transactional
    public TransferResponse create(TransferRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        if (transferRepository.existsByParcelIdAndStatusIn(parcel.getId(), BLOCKING)) {
            throw new BusinessRuleException("An active transfer already exists for this parcel");
        }
        LandOwnership current = ownershipRepository
                .findFirstByParcelIdAndStatusOrderByEffectiveDateDesc(parcel.getId(), OwnershipStatus.CURRENT)
                .orElseThrow(() -> new BusinessRuleException(
                        "No current ownership record exists for this parcel"));
        LandSale sale = null;
        if (req.saleId() != null) {
            sale = saleRepository.findById(req.saleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked sale not found"));
            if (!sale.getParcel().getId().equals(parcel.getId())) {
                throw new BusinessRuleException("Linked sale belongs to a different parcel");
            }
        }
        UserPrincipal actor = support.actor();
        LandTransfer t = new LandTransfer();
        t.setParcel(parcel);
        t.setTransferReference(nextRef());
        t.setPreviousOwnership(current);
        t.setPreviousOwnerName(current.getOwnerName());
        t.setNewOwnerName(req.newOwnerName());
        t.setNewOwnerReference(req.newOwnerReference());
        t.setSale(sale);
        t.setTitleReference(req.titleReference());
        t.setDocumentReference(req.documentReference());
        t.setStatus(TransferStatus.DRAFT);
        if (actor != null) {
            t.setCreatedBy(actor.getId());
            t.setCreatedByName(actor.getUsername());
        }
        t = transferRepository.save(t);
        support.tx(parcel, TransactionType.TRANSFER_CREATED, t.getId(), t.getTransferReference(),
                null, t.getStatus().name(), "Transfer to " + t.getNewOwnerName());
        return TransferResponse.from(t);
    }

    public PageResponse<TransferResponse> list(Long parcelId, TransferStatus status,
                                               String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.of(transferRepository.search(parcelId, status,
                LandParcelService.blank(search), pageable), TransferResponse::from);
    }

    public TransferResponse get(Long id) {
        return TransferResponse.from(find(id));
    }

    @Transactional
    public TransferResponse submit(Long id) {
        LandTransfer t = find(id);
        require(t.getStatus() == TransferStatus.DRAFT, "Only draft transfers can be submitted");
        if (t.getSale() != null) {
            SaleStatus saleStatus = t.getSale().getStatus();
            require(saleStatus == SaleStatus.LEGAL_PROCESSING || saleStatus == SaleStatus.COMPLETED,
                    "Linked sale must be in legal processing or completed before transfer submission");
        }
        t.setStatus(TransferStatus.SUBMITTED);
        t.setSubmittedAt(Instant.now());
        t = transferRepository.save(t);
        support.setParcelStatus(t.getParcel(), LandStatus.UNDER_TRANSFER,
                "Transfer " + t.getTransferReference() + " submitted");
        support.tx(t.getParcel(), TransactionType.TRANSFER_SUBMITTED, t.getId(),
                t.getTransferReference(), null, t.getStatus().name(), null);
        return TransferResponse.from(t);
    }

    @Transactional
    public TransferResponse approve(Long id, com.pcc.land.dto.request.DecisionRequest req) {
        LandTransfer t = find(id);
        require(t.getStatus() == TransferStatus.SUBMITTED, "Only submitted transfers can be approved");
        t.setStatus(TransferStatus.APPROVED);
        if (req != null) {
            t.setDecisionNote(req.note());
        }
        decide(t);
        t = transferRepository.save(t);
        support.tx(t.getParcel(), TransactionType.TRANSFER_APPROVED, t.getId(),
                t.getTransferReference(), null, t.getStatus().name(),
                req != null ? req.note() : null);
        return TransferResponse.from(t);
    }

    @Transactional
    public TransferResponse reject(Long id, com.pcc.land.dto.request.DecisionRequest req) {
        LandTransfer t = find(id);
        require(t.getStatus() == TransferStatus.SUBMITTED, "Only submitted transfers can be rejected");
        t.setStatus(TransferStatus.REJECTED);
        if (req != null) {
            t.setDecisionNote(req.note());
        }
        decide(t);
        t = transferRepository.save(t);
        releaseParcel(t, "Transfer " + t.getTransferReference() + " rejected");
        support.tx(t.getParcel(), TransactionType.TRANSFER_REJECTED, t.getId(),
                t.getTransferReference(), null, t.getStatus().name(),
                req != null ? req.note() : null);
        return TransferResponse.from(t);
    }

    /**
     * Legal completion: atomically ends the current ownership and opens the
     * new one. The whole handoff is one transaction — inconsistent ownership
     * states cannot be persisted.
     */
    @Transactional
    public TransferResponse complete(Long id, TransferCompleteRequest req) {
        LandTransfer t = find(id);
        require(t.getStatus() == TransferStatus.APPROVED,
                "Only approved transfers can be legally completed");
        UserPrincipal actor = support.actor();
        LandParcel parcel = t.getParcel();

        LandOwnership previous = t.getPreviousOwnership();
        previous.setStatus(OwnershipStatus.ENDED);
        previous.setEndDate(req.legalCompletionDate());
        ownershipRepository.save(previous);
        ownershipService.event(parcel, previous, OwnershipEventType.TRANSFERRED_OUT,
                previous.getOwnerName(), t.getNewOwnerName(), req.legalCompletionDate(),
                "Ownership transferred via " + t.getTransferReference(), actor);

        LandOwnership next = ownershipService.createRecord(parcel, t.getNewOwnerName(),
                t.getNewOwnerReference(), t.getTitleReference(), req.legalCompletionDate(),
                t.getDocumentReference(), OwnershipEventType.TRANSFERRED_IN,
                previous.getOwnerName(), "Transfer " + t.getTransferReference() + " completed", actor);

        t.setStatus(TransferStatus.COMPLETED);
        t.setLegalCompletionDate(req.legalCompletionDate());
        decide(t);
        t = transferRepository.save(t);

        LandStatus parcelStatus = t.getSale() != null ? LandStatus.SOLD : LandStatus.AVAILABLE;
        support.setParcelStatus(parcel, parcelStatus,
                "Transfer " + t.getTransferReference() + " legally completed");
        support.tx(parcel, TransactionType.TRANSFER_COMPLETED, t.getId(), t.getTransferReference(),
                null, t.getStatus().name(),
                "Ownership " + previous.getOwnershipReference() + " -> " + next.getOwnershipReference());
        support.tx(parcel, TransactionType.OWNERSHIP_ENDED, previous.getId(),
                previous.getOwnershipReference(), null, previous.getStatus().name(), null);
        return TransferResponse.from(t);
    }

    @Transactional
    public TransferResponse cancel(Long id, com.pcc.land.dto.request.DecisionRequest req) {
        LandTransfer t = find(id);
        require(t.getStatus() == TransferStatus.DRAFT || t.getStatus() == TransferStatus.SUBMITTED,
                "Only draft or submitted transfers can be cancelled");
        t.setStatus(TransferStatus.CANCELLED);
        if (req != null) {
            t.setDecisionNote(req.note());
        }
        decide(t);
        t = transferRepository.save(t);
        releaseParcel(t, "Transfer " + t.getTransferReference() + " cancelled");
        support.tx(t.getParcel(), TransactionType.TRANSFER_CANCELLED, t.getId(),
                t.getTransferReference(), null, t.getStatus().name(),
                req != null ? req.note() : null);
        return TransferResponse.from(t);
    }

    private void releaseParcel(LandTransfer t, String reason) {
        if (t.getParcel().getStatus() != LandStatus.UNDER_TRANSFER) {
            return;
        }
        LandStatus restore = t.getSale() != null
                && (t.getSale().getStatus() == SaleStatus.COMPLETED
                    || t.getSale().getStatus() == SaleStatus.LEGAL_PROCESSING)
                ? LandStatus.SOLD : LandStatus.AVAILABLE;
        support.setParcelStatus(t.getParcel(), restore, reason);
    }

    private void decide(LandTransfer t) {
        UserPrincipal actor = support.actor();
        if (actor != null) {
            t.setDecidedBy(actor.getId());
            t.setDecidedByName(actor.getUsername());
        }
        t.setDecidedAt(Instant.now());
    }

    private LandTransfer find(Long id) {
        return transferRepository.findById(id)
                .filter(t -> t.getParcel().getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessRuleException(message);
        }
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("TRF-", transferRepository.maxTransferSequence() + 1);
        } while (transferRepository.existsByTransferReference(ref));
        return ref;
    }
}
