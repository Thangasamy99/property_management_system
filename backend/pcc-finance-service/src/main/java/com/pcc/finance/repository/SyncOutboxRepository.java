package com.pcc.finance.repository;

import com.pcc.finance.entity.SyncOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyncOutboxRepository extends JpaRepository<SyncOutbox, Long> {

    Optional<SyncOutbox> findBySyncRef(String syncRef);

    @Query("SELECT o FROM SyncOutbox o WHERE o.status <> 'SUCCESS' AND o.permanent = false "
            + "AND o.retryCount < :maxRetries ORDER BY o.createdAt")
    List<SyncOutbox> findRetryable(@Param("maxRetries") int maxRetries);

    List<SyncOutbox> findBySyncRefStartingWithOrderByIdDesc(String prefix);

    @Query("SELECT o FROM SyncOutbox o WHERE o.syncRef LIKE CONCAT('%', :ref, '%') ORDER BY o.id")
    List<SyncOutbox> findRelatedTo(@Param("ref") String ref);
}
