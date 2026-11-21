package com.pcc.workflow.repository;

import com.pcc.workflow.entity.ApprovalTask;
import com.pcc.workflow.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {

    List<ApprovalTask> findByInstanceIdOrderByLevelOrder(Long instanceId);
    Optional<ApprovalTask> findByInstanceIdAndLevelOrderAndStatus(Long instanceId,
                                                                int levelOrder, TaskStatus status);
    long countByInstanceIdAndLevelOrder(Long instanceId, int levelOrder);

    @Query("SELECT t FROM ApprovalTask t JOIN FETCH t.instance i WHERE t.status = 'PENDING' "
            + "AND (t.assigneeId = :userId "
            + "     OR (t.assigneeId IS NULL AND t.assigneeRole IN :roles) "
            + "     OR EXISTS (SELECT d FROM WorkflowDelegation d WHERE d.delegateId = :userId "
            + "                AND d.status = 'ACTIVE' AND d.startDate <= CURRENT_DATE "
            + "                AND d.endDate >= CURRENT_DATE "
            + "                AND (d.module IS NULL OR d.module = i.module) "
            + "                AND (d.definitionId IS NULL OR d.definitionId = i.definition.id) "
            + "                AND (t.assigneeId = d.delegatorId "
            + "                     OR (t.assigneeId IS NULL AND t.assigneeRole IS NOT NULL))))")
    Page<ApprovalTask> pendingFor(@Param("userId") Long userId,
                                  @Param("roles") List<String> roles, Pageable pageable);

    @Query("SELECT t FROM ApprovalTask t WHERE t.status = 'PENDING' AND t.dueAt IS NOT NULL "
            + "AND t.dueAt < CURRENT_TIMESTAMP")
    List<ApprovalTask> overdue();
}
