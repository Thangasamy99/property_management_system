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

    /** Committed = recorded or received money still occupying the target balance. */
    @Query("SELECT COALESCE(SUM(a.amount),0) FROM PaymentAllocation a " +
            "WHERE a.target.id = :targetId AND a.payment.status IN ('RECORDED','POSTED','PARTIALLY_REFUNDED','REFUNDED')")
    BigDecimal sumCommittedForTarget(@Param("targetId") Long targetId);

    /** Posted = money actually received against the target. */
    @Query("SELECT COALESCE(SUM(a.amount),0) FROM PaymentAllocation a " +
            "WHERE a.target.id = :targetId AND a.payment.status IN ('POSTED','PARTIALLY_REFUNDED','REFUNDED')")
    BigDecimal sumPostedForTarget(@Param("targetId") Long targetId);

    /** Refunded money tied to this target's payments (1:1 allocation assumption). */
    @Query("SELECT COALESCE(SUM(p.refundedAmount),0) FROM Payment p " +
            "WHERE EXISTS (SELECT 1 FROM PaymentAllocation a WHERE a.payment = p AND a.target.id = :targetId)")
    BigDecimal sumRefundedForTarget(@Param("targetId") Long targetId);
}
