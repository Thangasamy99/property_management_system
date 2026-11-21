package com.pcc.finance.repository;

import com.pcc.finance.entity.Refund;
import com.pcc.finance.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.refundRef, 5) AS long)), 0) FROM Refund r WHERE r.refundRef LIKE 'RFD-%'")
    long maxRefundSequence();
    Optional<Refund> findByRefundRef(String ref);

    @Query("SELECT r FROM Refund r JOIN FETCH r.payment p WHERE " +
            "(:status IS NULL OR r.status = :status) " +
            "AND (:q IS NULL OR LOWER(r.refundRef) LIKE :q OR LOWER(p.paymentRef) LIKE :q " +
            "     OR LOWER(p.payerName) LIKE :q) ORDER BY r.requestedAt DESC")
    Page<Refund> search(@Param("status") RefundStatus status, @Param("q") String q, Pageable pageable);
}
