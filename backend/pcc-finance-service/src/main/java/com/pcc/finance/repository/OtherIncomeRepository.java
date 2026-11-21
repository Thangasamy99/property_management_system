package com.pcc.finance.repository;

import com.pcc.finance.entity.OtherIncome;
import com.pcc.finance.enums.IncomeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface OtherIncomeRepository extends JpaRepository<OtherIncome, Long> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.incomeRef, 5) AS long)), 0) FROM OtherIncome i WHERE i.incomeRef LIKE 'INC-%'")
    long maxIncomeSequence();
    Optional<OtherIncome> findByIncomeRef(String ref);

    @Query("SELECT i FROM OtherIncome i JOIN FETCH i.paymentMethod " +
            "WHERE (:status IS NULL OR i.status = :status) " +
            "AND (:category IS NULL OR i.category = :category) " +
            "AND (:from IS NULL OR i.incomeDate >= :from) AND (:to IS NULL OR i.incomeDate <= :to) " +
            "AND (:q IS NULL OR LOWER(i.incomeRef) LIKE :q OR LOWER(i.description) LIKE :q " +
            "     OR LOWER(i.payerName) LIKE :q) ORDER BY i.createdAt DESC")
    Page<OtherIncome> search(@Param("status") IncomeStatus status, @Param("category") String category,
                             @Param("from") LocalDate from, @Param("to") LocalDate to,
                             @Param("q") String q, Pageable pageable);
}
