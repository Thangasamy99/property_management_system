package com.pcc.finance.repository;

import com.pcc.finance.entity.Expense;
import com.pcc.finance.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(e.expenseRef, 5) AS long)), 0) FROM Expense e WHERE e.expenseRef LIKE 'EXP-%'")
    long maxExpenseSequence();
    Optional<Expense> findByExpenseRef(String ref);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.paymentMethod " +
            "WHERE (:status IS NULL OR e.status = :status) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:from IS NULL OR e.expenseDate >= :from) AND (:to IS NULL OR e.expenseDate <= :to) " +
            "AND (:q IS NULL OR LOWER(e.expenseRef) LIKE :q OR LOWER(e.description) LIKE :q " +
            "     OR LOWER(e.vendorName) LIKE :q OR LOWER(e.department) LIKE :q) ORDER BY e.createdAt DESC")
    Page<Expense> search(@Param("status") ExpenseStatus status, @Param("category") String category,
                         @Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("q") String q, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.status = 'PAID' " +
            "AND e.paidDate BETWEEN :from AND :to")
    BigDecimal sumPaidBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.category, COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.status = 'PAID' " +
            "AND e.paidDate BETWEEN :from AND :to GROUP BY e.category")
    List<Object[]> sumPaidByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
