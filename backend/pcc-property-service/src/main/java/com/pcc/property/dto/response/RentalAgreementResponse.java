package com.pcc.property.dto.response;

import com.pcc.property.entity.RentalAgreement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RentalAgreementResponse(
        Long id, String agreementNumber,
        Long apartmentId, String apartmentNumber, String buildingName, String propertyName,
        Long tenantId, String tenantName, String tenantCode,
        LocalDate startDate, LocalDate endDate,
        BigDecimal monthlyRent, BigDecimal deposit,
        String paymentFrequency, Integer rentDueDay,
        String status, String terminationReason,
        List<String> terms,
        String createdByName, Instant createdAt
) {
    public static RentalAgreementResponse of(RentalAgreement a) {
        var apt = a.getApartment();
        var tenant = a.getTenant();
        return new RentalAgreementResponse(
                a.getId(), a.getAgreementNumber(),
                apt.getId(), apt.getApartmentNumber(), apt.getBuilding().getName(), apt.getBuilding().getProperty().getName(),
                tenant.getId(), tenant.getName(), tenant.getCode(),
                a.getStartDate(), a.getEndDate(), a.getMonthlyRent(), a.getDeposit(),
                a.getPaymentFrequency().name(), a.getRentDueDay(),
                a.getStatus().name(), a.getTerminationReason(),
                a.getTerms().stream().map(t -> t.getContent()).toList(),
                a.getCreatedByName(), a.getCreatedAt());
    }
}
