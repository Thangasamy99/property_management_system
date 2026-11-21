package com.pcc.property.repository;

import com.pcc.property.entity.RentalAgreement;
import com.pcc.property.enums.AgreementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long> {

    boolean existsByAgreementNumber(String agreementNumber);

    @EntityGraph(attributePaths = {"apartment", "apartment.building", "tenant", "terms"})
    Optional<RentalAgreement> findWithDetailsById(Long id);

    /** Overlap check: any non-terminal agreement on the same apartment overlapping [start, end]. */
    @Query("""
            SELECT COUNT(a) > 0 FROM RentalAgreement a
            WHERE a.apartment.id = :apartmentId
              AND a.status IN :activeStatuses
              AND a.startDate <= :end AND a.endDate >= :start
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean overlapsActive(@Param("apartmentId") Long apartmentId,
                           @Param("start") java.time.LocalDate start,
                           @Param("end") java.time.LocalDate end,
                           @Param("activeStatuses") Collection<AgreementStatus> activeStatuses,
                           @Param("excludeId") Long excludeId);

    @Query("""
            SELECT a FROM RentalAgreement a
              JOIN FETCH a.apartment ap JOIN FETCH ap.building b JOIN FETCH a.tenant t
            WHERE (:apartmentId IS NULL OR ap.id = :apartmentId)
              AND (:tenantId IS NULL OR t.id = :tenantId)
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<RentalAgreement> search(@Param("apartmentId") Long apartmentId,
                                 @Param("tenantId") Long tenantId,
                                 @Param("status") AgreementStatus status,
                                 Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.agreementNumber, 5) AS long)), 0) FROM RentalAgreement a WHERE a.agreementNumber LIKE 'AGR-%'")
    long maxAgreementSequence();
}
