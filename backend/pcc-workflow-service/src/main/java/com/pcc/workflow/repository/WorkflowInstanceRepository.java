package com.pcc.workflow.repository;

import com.pcc.workflow.entity.WorkflowInstance;
import com.pcc.workflow.enums.WfStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Optional<WorkflowInstance> findByWorkflowRef(String ref);

    @Query("SELECT i FROM WorkflowInstance i WHERE "
            + "(:status IS NULL OR i.status = :status) AND "
            + "(:module IS NULL OR i.module = :module) AND "
            + "(:type IS NULL OR i.workflowType = :type) AND "
            + "(:submitter IS NULL OR i.submittedById = :submitter) AND "
            + "(:priority IS NULL OR i.priority = :priority) AND "
            + "(CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND "
            + "(CAST(:to AS timestamp) IS NULL OR i.createdAt < :to) AND "
            + "(:like IS NULL OR LOWER(i.workflowRef) LIKE :like OR LOWER(i.description) LIKE :like "
            + "  OR LOWER(i.entityRef) LIKE :like)")
    Page<WorkflowInstance> search(@Param("status") WfStatus status,
            @Param("module") String module, @Param("type") String type,
            @Param("submitter") Long submitter, @Param("priority") String priority,
            @Param("from") Instant from, @Param("to") Instant to,
            @Param("like") String like, Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(workflow_ref,4) AS UNSIGNED)),0) "
            + "FROM workflow_instances WHERE workflow_ref LIKE 'WF-%'", nativeQuery = true)
    long maxWorkflowSequence();

    @Query("SELECT i FROM WorkflowInstance i WHERE i.status IN "
            + "('SUBMITTED','PENDING_APPROVAL') AND i.dueAt IS NOT NULL AND i.dueAt < :now")
    List<WorkflowInstance> overdue(@Param("now") Instant now);

    @Query("SELECT i FROM WorkflowInstance i WHERE i.status IN "
            + "('SUBMITTED','PENDING_APPROVAL') AND i.dueAt IS NOT NULL AND i.dueAt < :now")
    Page<WorkflowInstance> overduePage(@Param("now") Instant now, Pageable p);

    long countByStatus(WfStatus status);
    List<WorkflowInstance> findByEntityTypeAndEntityRef(String type, String ref);
}
