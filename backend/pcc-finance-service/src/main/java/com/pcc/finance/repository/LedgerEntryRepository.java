package com.pcc.finance.repository;

import com.pcc.finance.entity.LedgerEntry;
import com.pcc.finance.enums.LedgerDirection;
import com.pcc.finance.enums.LedgerTxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT l FROM LedgerEntry l LEFT JOIN FETCH l.account WHERE " +
            "(:type IS NULL OR l.transactionType = :type) " +
            "AND (:direction IS NULL OR l.direction = :direction) " +
            "AND (:accountId IS NULL OR l.account.id = :accountId) " +
            "AND (:from IS NULL OR l.transactionDate >= :from) AND (:to IS NULL OR l.transactionDate <= :to) " +
            "AND (:q IS NULL OR LOWER(l.entryRef) LIKE :q OR LOWER(l.sourceRef) LIKE :q " +
            "     OR LOWER(l.description) LIKE :q) ORDER BY l.postedAt DESC")
    Page<LedgerEntry> search(@Param("type") LedgerTxType type, @Param("direction") LedgerDirection direction,
                             @Param("accountId") Long accountId,
                             @Param("from") LocalDate from, @Param("to") LocalDate to,
                             @Param("q") String q, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.direction='CREDIT' THEN l.amount ELSE -l.amount END),0) " +
            "FROM LedgerEntry l WHERE l.account.id = :accountId AND l.postingStatus = 'POSTED'")
    BigDecimal balanceForAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.direction='CREDIT' THEN l.amount ELSE -l.amount END),0) " +
            "FROM LedgerEntry l WHERE l.postingStatus = 'POSTED' AND l.account.accountType = 'CASH'")
    BigDecimal cashBalance();

    @Query("SELECT COALESCE(SUM(CASE WHEN l.direction='CREDIT' THEN l.amount ELSE -l.amount END),0) " +
            "FROM LedgerEntry l WHERE l.postingStatus = 'POSTED' AND l.account.accountType = 'BANK'")
    BigDecimal bankBalance();

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(l.entryRef, 5) AS long)), 0) FROM LedgerEntry l WHERE l.entryRef LIKE 'LED-%'")
    long maxLedgerSequence();
    List<LedgerEntry> findByPaymentId(Long paymentId);
}
