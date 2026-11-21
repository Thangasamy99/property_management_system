package com.pcc.land.repository;

import com.pcc.land.entity.LandTransfer;
import com.pcc.land.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface LandTransferRepository extends JpaRepository<LandTransfer, Long> {

    boolean existsByTransferReference(String transferReference);

    boolean existsByParcelIdAndStatusIn(Long parcelId, Collection<TransferStatus> statuses);

    @Query("""
            SELECT t FROM LandTransfer t JOIN t.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:status IS NULL OR t.status = :status)
              AND (:search IS NULL OR LOWER(t.transferReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.newOwnerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandTransfer> search(@Param("parcelId") Long parcelId,
                              @Param("status") TransferStatus status,
                              @Param("search") String search,
                              Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(t.transferReference, 5) AS long)), 0) FROM LandTransfer t WHERE t.transferReference LIKE 'TRF-%'")
    long maxTransferSequence();
}
