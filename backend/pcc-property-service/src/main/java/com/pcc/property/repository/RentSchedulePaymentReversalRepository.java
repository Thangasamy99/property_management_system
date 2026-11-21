package com.pcc.property.repository;

import com.pcc.property.entity.RentSchedulePaymentReversal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentSchedulePaymentReversalRepository extends JpaRepository<RentSchedulePaymentReversal, Long> {

    boolean existsByReversalRef(String reversalRef);
}
