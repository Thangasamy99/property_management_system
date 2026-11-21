package com.pcc.property.repository;

import com.pcc.property.entity.Tenant;
import com.pcc.property.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsByCode(String code);

    @Query("""
            SELECT t FROM Tenant t
            WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<Tenant> search(@Param("search") String search,
                        @Param("status") TenantStatus status,
                        Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(t.code, 5) AS long)), 0) FROM Tenant t WHERE t.code LIKE 'TNT-%'")
    long maxTenantCodeSequence();
}
