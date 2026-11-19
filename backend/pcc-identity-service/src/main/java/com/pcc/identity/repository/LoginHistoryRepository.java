package com.pcc.identity.repository;

import com.pcc.identity.entity.LoginHistory;
import com.pcc.identity.enums.LoginEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByEventType(LoginEventType eventType);

    @Query("""
            SELECT h FROM LoginHistory h
            WHERE (:username IS NULL OR LOWER(h.username) LIKE :username)
              AND (:eventType IS NULL OR h.eventType = :eventType)
              AND (:from IS NULL OR h.createdAt >= :from)
              AND (:to IS NULL OR h.createdAt < :to)
            ORDER BY h.createdAt DESC
            """)
    Page<LoginHistory> search(@Param("username") String username,
                              @Param("eventType") LoginEventType eventType,
                              @Param("from") java.time.Instant from,
                              @Param("to") java.time.Instant to,
                              Pageable pageable);

    @Query("""
            SELECT CAST(h.createdAt AS LocalDate), h.eventType, COUNT(h)
            FROM LoginHistory h
            WHERE h.createdAt >= :since
            GROUP BY CAST(h.createdAt AS LocalDate), h.eventType
            """)
    List<Object[]> countByDayAndType(@Param("since") java.time.Instant since);
}
