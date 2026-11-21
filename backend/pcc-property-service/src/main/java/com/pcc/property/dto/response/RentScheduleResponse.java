package com.pcc.property.dto.response;

import com.pcc.property.entity.RentSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentScheduleResponse(
        Long id, Long agreementId, LocalDate periodStart, LocalDate periodEnd,
        LocalDate dueDate, BigDecimal amount, BigDecimal paidAmount, String status
) {
    public static RentScheduleResponse of(RentSchedule s) {
        return new RentScheduleResponse(s.getId(), s.getAgreement().getId(),
                s.getPeriodStart(), s.getPeriodEnd(), s.getDueDate(), s.getAmount(),
                s.getPaidAmount(), s.getStatus().name());
    }
}
