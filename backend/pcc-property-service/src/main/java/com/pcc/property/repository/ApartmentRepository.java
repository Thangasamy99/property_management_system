package com.pcc.property.repository;

import com.pcc.property.entity.Apartment;
import com.pcc.property.enums.ApartmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    boolean existsByBuildingIdAndApartmentNumber(Long buildingId, String apartmentNumber);

    List<Apartment> findByBuildingId(Long buildingId);

    @Query("""
            SELECT a FROM Apartment a JOIN FETCH a.building b JOIN FETCH b.property p
            WHERE (:buildingId IS NULL OR b.id = :buildingId)
              AND (:search IS NULL OR LOWER(a.apartmentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<Apartment> search(@Param("buildingId") Long buildingId,
                           @Param("search") String search,
                           @Param("status") ApartmentStatus status,
                           Pageable pageable);
}
