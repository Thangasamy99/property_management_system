package com.pcc.land.dto.response;

import com.pcc.land.entity.LandTransfer;

import java.time.Instant;
import java.time.LocalDate;

public record TransferResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String transferReference,
        Long previousOwnershipId,
        String previousOwnerName,
        String newOwnerName,
        String newOwnerReference,
        Long saleId,
        String saleReference,
        String titleReference,
        String documentReference,
        String status,
        LocalDate legalCompletionDate,
        Instant submittedAt,
        String decidedByName,
        Instant decidedAt,
        String decisionNote,
        String createdByName,
        Instant createdAt
) {
    public static TransferResponse from(LandTransfer t) {
        return new TransferResponse(t.getId(), t.getParcel().getId(), t.getParcel().getParcelReference(),
                t.getParcel().getName(), t.getTransferReference(),
                t.getPreviousOwnership() != null ? t.getPreviousOwnership().getId() : null,
                t.getPreviousOwnerName(), t.getNewOwnerName(), t.getNewOwnerReference(),
                t.getSale() != null ? t.getSale().getId() : null,
                t.getSale() != null ? t.getSale().getSaleReference() : null,
                t.getTitleReference(), t.getDocumentReference(), t.getStatus().name(),
                t.getLegalCompletionDate(), t.getSubmittedAt(), t.getDecidedByName(), t.getDecidedAt(),
                t.getDecisionNote(), t.getCreatedByName(), t.getCreatedAt());
    }
}
