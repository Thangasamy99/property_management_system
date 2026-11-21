package com.pcc.workflow.repository;

import com.pcc.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    Optional<WorkflowDefinition> findByCode(String code);
    List<WorkflowDefinition> findByModule(String module);
    List<WorkflowDefinition> findByModuleAndActiveTrue(String module);
}
