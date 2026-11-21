package com.pcc.land.repository;

import com.pcc.land.entity.LandOwnership;
import com.pcc.land.enums.OwnershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LandOwnershipRepository extends JpaRepository<LandOwnership, Long> {

    boolean existsByOwnershipReference(String ownershipReference);

    List<LandOwnership> findByParcelIdAndStatus(Long parcelId, OwnershipStatus status);

    List<LandOwnership> findByParcelIdOrderByEffectiveDateDesc(Long parcelId);

    @Query("""
            SELECT o FROM LandOwnership o JOIN o.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:status IS NULL OR o.status = :status)
              AND (:search IS NULL OR LOWER(o.ownerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.ownershipReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandOwnership> search(@Param("parcelId") Long parcelId,
                               @Param("status") OwnershipStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.ownershipReference, 5) AS long)), 0) FROM LandOwnership o WHERE o.ownershipReference LIKE 'OWN-%'")
    long maxOwnershipSequence();

    Optional<LandOwnership> findFirstByParcelIdAndStatusOrderByEffectiveDateDesc(Long parcelId, OwnershipStatus status);
}
