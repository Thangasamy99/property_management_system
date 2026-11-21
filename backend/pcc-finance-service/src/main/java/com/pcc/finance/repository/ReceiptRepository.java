package com.pcc.finance.repository;

import com.pcc.finance.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.receiptNo, 5) AS long)), 0) FROM Receipt r WHERE r.receiptNo LIKE 'RCP-%'")
    long maxReceiptSequence();
    Optional<Receipt> findByReceiptNo(String receiptNo);

    Optional<Receipt> findByPaymentId(Long paymentId);

    @Query("SELECT r FROM Receipt r WHERE " +
            "(:from IS NULL OR r.paymentDate >= :from) AND (:to IS NULL OR r.paymentDate <= :to) " +
            "AND (:q IS NULL OR LOWER(r.receiptNo) LIKE :q OR LOWER(r.payerName) LIKE :q " +
            "     OR LOWER(r.relatedReference) LIKE :q OR LOWER(r.payment.paymentRef) LIKE :q) " +
            "ORDER BY r.issuedAt DESC")
    Page<Receipt> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("q") String q, Pageable pageable);
}
