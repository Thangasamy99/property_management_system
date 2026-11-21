package com.pcc.property.repository;

import com.pcc.property.entity.Building;
import com.pcc.property.enums.BuildingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    boolean existsByCode(String code);

    List<Building> findByPropertyId(Long propertyId);

    @Query("""
            SELECT b FROM Building b JOIN FETCH b.property p
            WHERE (:propertyId IS NULL OR p.id = :propertyId)
              AND (:search IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(b.code) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<Building> search(@Param("propertyId") Long propertyId,
                          @Param("search") String search,
                          @Param("status") BuildingStatus status,
                          Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(b.code, 5) AS long)), 0) FROM Building b WHERE b.code LIKE 'BLD-%'")
    long maxBuildingCodeSequence();
}
