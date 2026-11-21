package com.pcc.land.repository;

import com.pcc.land.entity.LandAcquisition;
import com.pcc.land.enums.AcquisitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LandAcquisitionRepository extends JpaRepository<LandAcquisition, Long> {

    boolean existsByAcquisitionReference(String acquisitionReference);

    @Query("""
            SELECT a FROM LandAcquisition a JOIN a.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:status IS NULL OR a.status = :status)
              AND (:search IS NULL OR LOWER(a.acquisitionReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.sellerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandAcquisition> search(@Param("parcelId") Long parcelId,
                                 @Param("status") AcquisitionStatus status,
                                 @Param("search") String search,
                                 Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.acquisitionReference, 5) AS long)), 0) FROM LandAcquisition a WHERE a.acquisitionReference LIKE 'ACQ-%'")
    long maxAcquisitionSequence();
}
