package com.pcc.workflow.repository;

import com.pcc.workflow.entity.IntegrationOutbox;
import com.pcc.workflow.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IntegrationOutboxRepository extends JpaRepository<IntegrationOutbox, Long> {
    List<IntegrationOutbox> findByInstanceId(Long instanceId);
    Optional<IntegrationOutbox> findByIntegrationRef(String ref);
    List<IntegrationOutbox> findByStatusInOrderByCreatedAt(List<OutboxStatus> statuses);
}
