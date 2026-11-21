package com.pcc.workflow.repository;

import com.pcc.workflow.entity.WorkflowAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowAuditLogRepository extends JpaRepository<WorkflowAuditLog, Long> {
    List<WorkflowAuditLog> findByInstanceIdOrderByCreatedAt(Long instanceId);
}
