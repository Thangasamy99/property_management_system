package com.pcc.land.repository;

import com.pcc.land.entity.LandSale;
import com.pcc.land.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface LandSaleRepository extends JpaRepository<LandSale, Long> {

    boolean existsBySaleReference(String saleReference);

    boolean existsByParcelIdAndStatusIn(Long parcelId, Collection<SaleStatus> statuses);


    @Query("""
            SELECT s FROM LandSale s JOIN s.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:status IS NULL OR s.status = :status)
              AND (:search IS NULL OR LOWER(s.saleReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.buyerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandSale> search(@Param("parcelId") Long parcelId,
                          @Param("status") SaleStatus status,
                          @Param("search") String search,
                          Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.saleReference, 5) AS long)), 0) FROM LandSale s WHERE s.saleReference LIKE 'SAL-%'")
    long maxSaleSequence();
}
