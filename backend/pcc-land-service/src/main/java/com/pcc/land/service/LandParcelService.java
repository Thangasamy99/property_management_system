package com.pcc.land.service;

import com.pcc.land.dto.request.ParcelRequest;
import com.pcc.land.dto.request.ParcelStatusRequest;
import com.pcc.land.dto.response.LandSummaryResponse;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.ParcelResponse;
import com.pcc.land.dto.response.StatusHistoryResponse;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.entity.LandStatusHistory;
import com.pcc.land.enums.LandClassification;
import com.pcc.land.enums.LandStatus;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.DuplicateResourceException;
import com.pcc.land.repository.*;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandParcelService {

    private final LandParcelRepository parcelRepository;
    private final LandStatusHistoryRepository statusHistoryRepository;
    private final LandSaleRepository saleRepository;
    private final LandTransferRepository transferRepository;
    private final LandSupport support;

    @Transactional
    public ParcelResponse register(ParcelRequest req) {
        UserPrincipal actor = support.actor();
        LandParcel p = new LandParcel();
        apply(p, req);
        p.setParcelReference(nextRef());
        p.setStatus(LandStatus.AVAILABLE);
        if (actor != null) {
            p.setCreatedBy(actor.getId());
            p.setCreatedByName(actor.getUsername());
        }
        p = parcelRepository.save(p);
        recordStatus(p, null, LandStatus.AVAILABLE, "Parcel registered", actor);
        support.tx(p, TransactionType.PARCEL_REGISTERED, p.getId(), p.getParcelReference(),
                null, p.getStatus().name(), "Land parcel registered");
        return ParcelResponse.from(p);
    }

    public PageResponse<ParcelResponse> list(String search, LandStatus status,
                                             LandClassification classification, String region,
                                             int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(sort == null || sort.isBlank() ? "createdAt" : sort).descending());
        return PageResponse.of(parcelRepository.search(blank(search), status, classification, blank(region), pageable),
                ParcelResponse::from);
    }

    public ParcelResponse get(Long id) {
        return ParcelResponse.from(support.parcel(id));
    }

    @Transactional
    public ParcelResponse update(Long id, ParcelRequest req) {
        LandParcel p = support.parcel(id);
        apply(p, req);
        return ParcelResponse.from(parcelRepository.save(p));
    }

    @Transactional
    public ParcelResponse changeStatus(Long id, ParcelStatusRequest req) {
        LandParcel p = support.parcel(id);
        UserPrincipal actor = support.actor();
        LandStatus previous = p.getStatus();
        if (previous == req.status()) {
            return ParcelResponse.from(p);
        }
        p.setStatus(req.status());
        p = parcelRepository.save(p);
        recordStatus(p, previous, req.status(), req.reason(), actor);
        support.tx(p, TransactionType.STATUS_CHANGED, p.getId(), p.getParcelReference(),
                null, req.status().name(), req.reason());
        return ParcelResponse.from(p);
    }

    @Transactional
    public void delete(Long id) {
        LandParcel p = support.parcel(id);
        p.setDeletedAt(Instant.now());
        parcelRepository.save(p);
        support.tx(p, TransactionType.STATUS_CHANGED, p.getId(), p.getParcelReference(),
                null, "DELETED", "Parcel archived");
    }

    public java.util.List<StatusHistoryResponse> statusHistory(Long parcelId) {
        support.parcel(parcelId);
        return statusHistoryRepository.findByParcelIdOrderByChangedAtDesc(parcelId)
                .stream().map(StatusHistoryResponse::from).toList();
    }

    public LandSummaryResponse summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (LandStatus s : LandStatus.values()) {
            byStatus.put(s.name(), parcelRepository.countByStatusAndDeletedAtIsNull(s));
        }
        BigDecimal totalArea = parcelRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(LandParcel::getAreaSqm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingTransfers = transferRepository.findAll().stream()
                .filter(t -> t.getStatus() == com.pcc.land.enums.TransferStatus.SUBMITTED
                        || t.getStatus() == com.pcc.land.enums.TransferStatus.APPROVED)
                .count();
        return new LandSummaryResponse(parcelRepository.countByDeletedAtIsNull(), totalArea,
                byStatus, saleRepository.count(), pendingTransfers);
    }

    private void apply(LandParcel p, ParcelRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setClassification(req.classification());
        p.setAreaSqm(req.unit().toSqm(req.areaValue()));
        p.setOriginalArea(req.areaValue());
        p.setOriginalUnit(req.unit());
        p.setRegion(req.region());
        p.setDivision(req.division());
        p.setSubdivision(req.subdivision());
        p.setLocality(req.locality());
        p.setAddress(req.address());
        p.setAcquisitionDate(req.acquisitionDate());
    }

    private void recordStatus(LandParcel p, LandStatus from, LandStatus to, String reason, UserPrincipal actor) {
        LandStatusHistory h = new LandStatusHistory();
        h.setParcel(p);
        h.setPreviousStatus(from);
        h.setNewStatus(to);
        h.setReason(reason);
        if (actor != null) {
            h.setChangedBy(actor.getId());
            h.setChangedByName(actor.getUsername());
        }
        statusHistoryRepository.save(h);
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("LND-", parcelRepository.maxParcelSequence() + 1);
        } while (parcelRepository.existsByParcelReference(ref));
        return ref;
    }

    static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
