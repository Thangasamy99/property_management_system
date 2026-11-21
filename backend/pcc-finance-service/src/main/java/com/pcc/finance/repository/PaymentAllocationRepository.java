package com.pcc.finance.repository;

import com.pcc.finance.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    List<PaymentAllocation> findByPaymentId(Long paymentId);

    /** All allocations on posted payments — used for downstream sync reconciliation. */
    @Query("SELECT a FROM PaymentAllocation a JOIN FETCH a.target JOIN FETCH a.payment " +
            "WHERE a.payment.status IN ('POSTED','PARTIALLY_REFUNDED','REFUNDED','REVERSED')")
    List<PaymentAllocation> findPostedAllocations();

    /** Committed = recorded or received money still occupying the target balance. */
    @Query("SELECT COALESCE(SUM(a.amount - a.reversedAmount),0) FROM PaymentAllocation a " +
            "WHERE a.target.id = :targetId AND a.payment.status IN ('RECORDED','POSTED','PARTIALLY_REFUNDED','REFUNDED')")
    BigDecimal sumCommittedForTarget(@Param("targetId") Long targetId);

    /** Posted = money actually received against the target (gross, before reversals). */
    @Query("SELECT COALESCE(SUM(a.amount),0) FROM PaymentAllocation a " +
            "WHERE a.target.id = :targetId AND a.payment.status IN ('POSTED','PARTIALLY_REFUNDED','REFUNDED')")
    BigDecimal sumPostedForTarget(@Param("targetId") Long targetId);

    /** Net posted = posted allocations minus recorded reversals (allocation-level). */
    @Query("SELECT COALESCE(SUM(a.amount - a.reversedAmount),0) FROM PaymentAllocation a " +
            "WHERE a.target.id = :targetId AND a.payment.status IN ('POSTED','PARTIALLY_REFUNDED','REFUNDED')")
    BigDecimal sumPostedNetForTarget(@Param("targetId") Long targetId);
}
