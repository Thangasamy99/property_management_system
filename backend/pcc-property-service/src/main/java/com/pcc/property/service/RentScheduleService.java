package com.pcc.property.service;

import com.pcc.property.dto.response.RentScheduleResponse;
import com.pcc.property.entity.RentSchedule;
import com.pcc.property.enums.ScheduleStatus;
import com.pcc.property.exception.BusinessRuleException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.RentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RentScheduleService {

    private final RentScheduleRepository scheduleRepository;

    /**
     * Finance integration contract: records the total received for a schedule
     * and derives status (PARTIAL / PAID). Only callable with
     * RENT_SCHEDULE_PAYMENT_SYNC. CANCELLED schedules reject sync.
     */
    @Transactional
    public RentScheduleResponse recordPayment(Long id, BigDecimal paidAmount) {
        RentSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rent schedule not found"));
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled schedules cannot receive payments");
        }
        s.setPaidAmount(paidAmount.max(BigDecimal.ZERO));
        if (s.getPaidAmount().compareTo(s.getAmount()) >= 0) {
            s.setStatus(ScheduleStatus.PAID);
        } else if (s.getPaidAmount().signum() > 0) {
            s.setStatus(ScheduleStatus.PARTIAL);
        }
        return RentScheduleResponse.of(scheduleRepository.save(s));
    }
}
