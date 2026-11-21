package com.pcc.land.repository;

import com.pcc.land.entity.LandFinancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface LandFinancePaymentRepository extends JpaRepository<LandFinancePayment, Long> {

    Optional<LandFinancePayment> findByTargetTypeAndTargetIdAndAllocationRef(
            String targetType, Long targetId, String allocationRef);

    @Query("SELECT COALESCE(SUM(p.amount - p.reversedAmount), 0) FROM LandFinancePayment p " +
           "WHERE p.targetType = :type AND p.targetId = :targetId")
    BigDecimal netPaidForTarget(@Param("type") String targetType, @Param("targetId") Long targetId);
}
