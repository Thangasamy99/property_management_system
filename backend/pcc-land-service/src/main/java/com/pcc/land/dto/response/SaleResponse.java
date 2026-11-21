package com.pcc.land.dto.response;

import com.pcc.land.entity.LandSale;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SaleResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String saleReference,
        String buyerName,
        String buyerReference,
        String buyerContact,
        BigDecimal proposedPrice,
        BigDecimal agreedPrice,
        String agreementReference,
        LocalDate agreementDate,
        String documentReference,
        String status,
        BigDecimal paidAmount,
        String financeStatus,
        Instant submittedAt,
        String decidedByName,
        Instant decidedAt,
        String decisionNote,
        String createdByName,
        Instant createdAt
) {
    public static SaleResponse from(LandSale s) {
        return new SaleResponse(s.getId(), s.getParcel().getId(), s.getParcel().getParcelReference(),
                s.getParcel().getName(), s.getSaleReference(), s.getBuyerName(), s.getBuyerReference(),
                s.getBuyerContact(), s.getProposedPrice(), s.getAgreedPrice(), s.getAgreementReference(),
                s.getAgreementDate(), s.getDocumentReference(), s.getStatus().name(),
                s.getPaidAmount(), s.getFinanceStatus(), s.getSubmittedAt(),
                s.getDecidedByName(), s.getDecidedAt(), s.getDecisionNote(), s.getCreatedByName(), s.getCreatedAt());
    }
}
