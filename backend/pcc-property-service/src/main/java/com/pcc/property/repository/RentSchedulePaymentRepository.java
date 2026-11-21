package com.pcc.property.repository;

import com.pcc.property.entity.RentSchedulePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface RentSchedulePaymentRepository extends JpaRepository<RentSchedulePayment, Long> {

    Optional<RentSchedulePayment> findByScheduleIdAndAllocationRef(Long scheduleId, String allocationRef);

    @Query("SELECT COALESCE(SUM(p.amount - p.reversedAmount), 0) FROM RentSchedulePayment p WHERE p.schedule.id = :scheduleId")
    BigDecimal netPaidForSchedule(@Param("scheduleId") Long scheduleId);
}
