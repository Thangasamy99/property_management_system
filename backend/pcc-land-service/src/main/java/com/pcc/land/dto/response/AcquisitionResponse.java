package com.pcc.land.dto.response;

import com.pcc.land.entity.LandAcquisition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AcquisitionResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String acquisitionReference,
        String sellerName,
        String sellerReference,
        String sellerContact,
        LocalDate acquisitionDate,
        BigDecimal purchasePrice,
        String documentReference,
        String approvalReference,
        String status,
        String decidedByName,
        Instant decidedAt,
        String decisionNote,
        String createdByName,
        Instant createdAt
) {
    public static AcquisitionResponse from(LandAcquisition a) {
        return new AcquisitionResponse(a.getId(), a.getParcel().getId(), a.getParcel().getParcelReference(),
                a.getParcel().getName(), a.getAcquisitionReference(), a.getSellerName(), a.getSellerReference(),
                a.getSellerContact(), a.getAcquisitionDate(), a.getPurchasePrice(), a.getDocumentReference(),
                a.getApprovalReference(), a.getStatus().name(), a.getDecidedByName(), a.getDecidedAt(),
                a.getDecisionNote(), a.getCreatedByName(), a.getCreatedAt());
    }
}
