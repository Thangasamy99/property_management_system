package com.pcc.land.repository;

import com.pcc.land.entity.LandTransaction;
import com.pcc.land.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LandTransactionRepository extends JpaRepository<LandTransaction, Long> {

    List<LandTransaction> findByParcelIdOrderByCreatedAtDesc(Long parcelId);

    @Query("""
            SELECT t FROM LandTransaction t JOIN t.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:type IS NULL OR t.transactionType = :type)
              AND (:search IS NULL OR LOWER(t.referenceCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandTransaction> search(@Param("parcelId") Long parcelId,
                                 @Param("type") TransactionType type,
                                 @Param("search") String search,
                                 Pageable pageable);
}
