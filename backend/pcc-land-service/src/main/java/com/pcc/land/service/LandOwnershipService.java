package com.pcc.land.service;

import com.pcc.land.dto.request.OwnershipRequest;
import com.pcc.land.dto.response.OwnershipHistoryResponse;
import com.pcc.land.dto.response.OwnershipResponse;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.entity.LandOwnership;
import com.pcc.land.entity.LandOwnershipHistory;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.enums.OwnershipEventType;
import com.pcc.land.enums.OwnershipStatus;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.repository.LandOwnershipHistoryRepository;
import com.pcc.land.repository.LandOwnershipRepository;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandOwnershipService {

    private final LandOwnershipRepository ownershipRepository;
    private final LandOwnershipHistoryRepository historyRepository;
    private final LandSupport support;

    @Transactional
    public OwnershipResponse register(OwnershipRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        UserPrincipal actor = support.actor();
        LandOwnership o = new LandOwnership();
        o.setParcel(parcel);
        o.setOwnershipReference(nextRef());
        o.setOwnerName(req.ownerName());
        o.setOwnerReference(req.ownerReference());
        o.setTitleReference(req.titleReference());
        o.setSharePercent(req.sharePercent() != null ? req.sharePercent() : new BigDecimal("100.00"));
        o.setAcquisitionDate(req.acquisitionDate());
        o.setEffectiveDate(req.effectiveDate());
        o.setStatus(OwnershipStatus.CURRENT);
        o.setDocumentReference(req.documentReference());
        if (actor != null) {
            o.setCreatedBy(actor.getId());
            o.setCreatedByName(actor.getUsername());
        }
        o = ownershipRepository.save(o);
        event(parcel, o, OwnershipEventType.REGISTERED, null, o.getOwnerName(),
                o.getEffectiveDate(), "Ownership record registered", actor);
        support.tx(parcel, TransactionType.OWNERSHIP_RECORDED, o.getId(), o.getOwnershipReference(),
                null, o.getStatus().name(), "Ownership recorded for " + o.getOwnerName());
        return OwnershipResponse.from(o);
    }

    public PageResponse<OwnershipResponse> list(Long parcelId, OwnershipStatus status,
                                                String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("effectiveDate").descending());
        return PageResponse.of(ownershipRepository.search(parcelId, status,
                LandParcelService.blank(search), pageable), OwnershipResponse::from);
    }

    public List<OwnershipResponse> byParcel(Long parcelId) {
        support.parcel(parcelId);
        return ownershipRepository.findByParcelIdOrderByEffectiveDateDesc(parcelId)
                .stream().map(OwnershipResponse::from).toList();
    }

    public List<OwnershipHistoryResponse> history(Long parcelId) {
        support.parcel(parcelId);
        return historyRepository.findByParcelIdOrderByCreatedAtDesc(parcelId)
                .stream().map(OwnershipHistoryResponse::from).toList();
    }

    /** Create a CURRENT ownership record (used by transfer completion). */
    LandOwnership createRecord(LandParcel parcel, String ownerName, String ownerReference,
                               String titleReference, java.time.LocalDate effectiveDate,
                               String documentReference, OwnershipEventType eventType,
                               String previousOwner, String note, UserPrincipal actor) {
        LandOwnership o = new LandOwnership();
        o.setParcel(parcel);
        o.setOwnershipReference(nextRef());
        o.setOwnerName(ownerName);
        o.setOwnerReference(ownerReference);
        o.setTitleReference(titleReference);
        o.setEffectiveDate(effectiveDate);
        o.setStatus(OwnershipStatus.CURRENT);
        o.setDocumentReference(documentReference);
        if (actor != null) {
            o.setCreatedBy(actor.getId());
            o.setCreatedByName(actor.getUsername());
        }
        o = ownershipRepository.save(o);
        event(parcel, o, eventType, previousOwner, ownerName, effectiveDate, note, actor);
        return o;
    }

    void event(LandParcel parcel, LandOwnership ownership, OwnershipEventType type,
               String previousOwner, String newOwner, java.time.LocalDate effectiveDate,
               String note, UserPrincipal actor) {
        LandOwnershipHistory h = new LandOwnershipHistory();
        h.setParcel(parcel);
        h.setOwnership(ownership);
        h.setEventType(type);
        h.setPreviousOwner(previousOwner);
        h.setNewOwner(newOwner);
        h.setEffectiveDate(effectiveDate);
        h.setNote(note);
        if (actor != null) {
            h.setPerformedBy(actor.getId());
            h.setPerformedByName(actor.getUsername());
        }
        historyRepository.save(h);
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("OWN-", ownershipRepository.maxOwnershipSequence() + 1);
        } while (ownershipRepository.existsByOwnershipReference(ref));
        return ref;
    }
}
