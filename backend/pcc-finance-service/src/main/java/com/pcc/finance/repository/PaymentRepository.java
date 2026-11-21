package com.pcc.finance.repository;

import com.pcc.finance.entity.Payment;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.paymentRef, 5) AS long)), 0) FROM Payment p WHERE p.paymentRef LIKE 'PAY-%'")
    long maxPaymentSequence();
    Optional<Payment> findByPaymentRef(String ref);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT p FROM Payment p JOIN FETCH p.paymentMethod LEFT JOIN FETCH p.account " +
            "WHERE (:type IS NULL OR p.paymentType = :type) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:from IS NULL OR p.paymentDate >= :from) AND (:to IS NULL OR p.paymentDate <= :to) " +
            "AND (:q IS NULL OR LOWER(p.paymentRef) LIKE :q OR LOWER(p.payerName) LIKE :q " +
            "     OR LOWER(p.paymentReference) LIKE :q) " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> search(@Param("type") PaymentType type, @Param("status") PaymentStatus status,
                         @Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("q") String q, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.paymentType IN :types " +
            "AND p.status = 'POSTED' AND p.paymentDate BETWEEN :from AND :to")
    BigDecimal sumPosted(@Param("types") List<PaymentType> types,
                         @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT p.paymentType, COALESCE(SUM(p.amount),0) FROM Payment p " +
            "WHERE p.status = 'POSTED' AND p.paymentDate BETWEEN :from AND :to GROUP BY p.paymentType")
    List<Object[]> sumPostedByType(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Payment> findTop10ByStatusOrderByCreatedAtDesc(PaymentStatus status);
}
