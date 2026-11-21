package com.pcc.land.service;

import com.pcc.land.entity.LandParcel;
import com.pcc.land.entity.LandStatusHistory;
import com.pcc.land.entity.LandTransaction;
import com.pcc.land.enums.LandStatus;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.LandParcelRepository;
import com.pcc.land.repository.LandStatusHistoryRepository;
import com.pcc.land.repository.LandTransactionRepository;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Shared helpers: actor lookup, parcel lookup, ledger + code sequences. */
@Component
@RequiredArgsConstructor
public class LandSupport {

    private final LandParcelRepository parcelRepository;
    private final LandTransactionRepository transactionRepository;
    private final LandStatusHistoryRepository statusHistoryRepository;

    public UserPrincipal actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p : null;
    }

    public LandParcel parcel(Long id) {
        return parcelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Land parcel not found"));
    }

    public void tx(LandParcel parcel, TransactionType type, Long refId, String refCode,
                   BigDecimal amount, String status, String description) {
        UserPrincipal actor = actor();
        LandTransaction t = new LandTransaction();
        t.setParcel(parcel);
        t.setTransactionType(type);
        t.setReferenceId(refId);
        t.setReferenceCode(refCode);
        t.setAmount(amount);
        t.setStatus(status);
        t.setDescription(description);
        if (actor != null) {
            t.setActorId(actor.getId());
            t.setActorName(actor.getUsername());
        }
        transactionRepository.save(t);
    }

    /** Move a parcel to a new status, persisting status history. */
    public void setParcelStatus(LandParcel parcel, LandStatus to, String reason) {
        LandStatus from = parcel.getStatus();
        if (from == to) {
            return;
        }
        parcel.setStatus(to);
        parcelRepository.save(parcel);
        LandStatusHistory h = new LandStatusHistory();
        h.setParcel(parcel);
        h.setPreviousStatus(from);
        h.setNewStatus(to);
        h.setReason(reason);
        UserPrincipal actor = actor();
        if (actor != null) {
            h.setChangedBy(actor.getId());
            h.setChangedByName(actor.getUsername());
        }
        statusHistoryRepository.save(h);
    }

    public static String ref(String prefix, long seq) {
        return prefix + String.format("%04d", seq);
    }
}
