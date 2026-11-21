package com.pcc.property.repository;

import com.pcc.property.entity.MaintenanceRequest;
import com.pcc.property.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    boolean existsByRequestNumber(String requestNumber);

    @Query("""
            SELECT m FROM MaintenanceRequest m
              LEFT JOIN FETCH m.apartment a LEFT JOIN FETCH m.building b
            WHERE (:apartmentId IS NULL OR a.id = :apartmentId)
              AND (:buildingId IS NULL OR b.id = :buildingId)
              AND (:status IS NULL OR m.status = :status)
            """)
    Page<MaintenanceRequest> search(@Param("apartmentId") Long apartmentId,
                                    @Param("buildingId") Long buildingId,
                                    @Param("status") MaintenanceStatus status,
                                    Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(m.requestNumber, 5) AS long)), 0) FROM MaintenanceRequest m WHERE m.requestNumber LIKE 'MNT-%'")
    long maxRequestSequence();
}
