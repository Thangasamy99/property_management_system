package com.pcc.property.repository;

import com.pcc.property.entity.Property;
import com.pcc.property.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "address")
    Optional<Property> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "address")
    @Query("""
            SELECT p FROM Property p
            WHERE p.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR p.status = :status)
              AND (:type IS NULL OR p.propertyType = :type)
            """)
    Page<Property> search(@Param("search") String search,
                          @Param("status") PropertyStatus status,
                          @Param("type") com.pcc.property.enums.PropertyType type,
                          Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.code, 6) AS long)), 0) FROM Property p WHERE p.code LIKE 'PROP-%'")
    long maxPropertyCodeSequence();
}
