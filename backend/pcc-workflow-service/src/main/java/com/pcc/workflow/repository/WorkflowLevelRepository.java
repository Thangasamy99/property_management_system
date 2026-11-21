package com.pcc.workflow.repository;

import com.pcc.workflow.entity.WorkflowLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowLevelRepository extends JpaRepository<WorkflowLevel, Long> {
    List<WorkflowLevel> findByDefinitionIdOrderByLevelOrder(Long definitionId);
    Optional<WorkflowLevel> findByDefinitionIdAndLevelOrder(Long definitionId, int levelOrder);
    void deleteByDefinitionId(Long definitionId);
}
