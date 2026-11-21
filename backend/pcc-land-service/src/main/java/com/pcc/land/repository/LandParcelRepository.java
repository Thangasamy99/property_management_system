package com.pcc.land.repository;

import com.pcc.land.entity.LandParcel;
import com.pcc.land.enums.LandClassification;
import com.pcc.land.enums.LandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LandParcelRepository extends JpaRepository<LandParcel, Long> {

    boolean existsByParcelReference(String parcelReference);

    Optional<LandParcel> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            SELECT p FROM LandParcel p
            WHERE p.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.parcelReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.locality) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR p.status = :status)
              AND (:classification IS NULL OR p.classification = :classification)
              AND (:region IS NULL OR LOWER(p.region) LIKE LOWER(CONCAT('%', :region, '%')))
            """)
    Page<LandParcel> search(@Param("search") String search,
                            @Param("status") LandStatus status,
                            @Param("classification") LandClassification classification,
                            @Param("region") String region,
                            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.parcelReference, 5) AS long)), 0) FROM LandParcel p WHERE p.parcelReference LIKE 'LND-%'")
    long maxParcelSequence();

    long countByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(LandStatus status);
}
