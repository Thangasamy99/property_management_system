package com.pcc.workflow.service;

import com.pcc.workflow.dto.request.DefinitionRequest;
import com.pcc.workflow.dto.request.LevelRequest;
import com.pcc.workflow.dto.response.DefinitionResponse;
import com.pcc.workflow.entity.WorkflowDefinition;
import com.pcc.workflow.entity.WorkflowLevel;
import com.pcc.workflow.exception.DuplicateResourceException;
import com.pcc.workflow.exception.ResourceNotFoundException;
import com.pcc.workflow.repository.WorkflowDefinitionRepository;
import com.pcc.workflow.repository.WorkflowLevelRepository;
import com.pcc.workflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefinitionService {

    private final WorkflowDefinitionRepository definitions;
    private final WorkflowLevelRepository levels;

    @Transactional(readOnly = true)
    public List<DefinitionResponse> list(String module) {
        var list = module == null ? definitions.findAll() : definitions.findByModule(module);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DefinitionResponse detail(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public DefinitionResponse create(DefinitionRequest req, UserPrincipal actor) {
        definitions.findByCode(req.code()).ifPresent(d -> {
            throw new DuplicateResourceException("Definition code exists: " + req.code());
        });
        WorkflowDefinition d = new WorkflowDefinition();
        apply(d, req, actor.getId());
        d.setCreatedById(actor.getId());
        d = definitions.save(d);
        saveLevels(d, req.levels());
        return toResponse(d);
    }

    @Transactional
    public DefinitionResponse update(Long id, DefinitionRequest req, UserPrincipal actor) {
        WorkflowDefinition d = get(id);
        apply(d, req, actor.getId());
        d = definitions.save(d);
        levels.deleteByDefinitionId(d.getId());
        saveLevels(d, req.levels());
        return toResponse(d);
    }

    private void apply(WorkflowDefinition d, DefinitionRequest req, Long actorId) {
        d.setCode(req.code());
        d.setName(req.name());
        d.setDescription(req.description());
        d.setModule(req.module());
        d.setWorkflowType(req.workflowType());
        d.setEntityType(req.entityType());
        d.setIntegrationMethod(req.integrationMethod());
        d.setIntegrationPath(req.integrationPath());
        d.setEscalationDays(req.escalationDays());
        d.setActive(req.active() == null || req.active());
        d.setUpdatedById(actorId);
        d.setUpdatedAt(Instant.now());
    }

    private void saveLevels(WorkflowDefinition d, List<LevelRequest> reqs) {
        for (LevelRequest r : reqs) {
            WorkflowLevel l = new WorkflowLevel();
            l.setDefinition(d);
            l.setLevelOrder(r.levelOrder());
            l.setName(r.name());
            l.setApproverRole(r.approverRole());
            l.setApproverId(r.approverId());
            l.setMinAmount(r.minAmount());
            l.setMandatory(r.mandatory() == null || r.mandatory());
            levels.save(l);
        }
    }

    private WorkflowDefinition get(Long id) {
        return definitions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow definition not found"));
    }

    private DefinitionResponse toResponse(WorkflowDefinition d) {
        var levelDtos = levels.findByDefinitionIdOrderByLevelOrder(d.getId()).stream()
                .map(l -> new DefinitionResponse.LevelDto(l.getId(), l.getLevelOrder(), l.getName(),
                        l.getApproverRole(), l.getApproverId(), l.getMinAmount(), l.isMandatory()))
                .toList();
        return new DefinitionResponse(d.getId(), d.getCode(), d.getName(), d.getDescription(),
                d.getModule(), d.getWorkflowType(), d.getEntityType(), d.getIntegrationMethod(),
                d.getIntegrationPath(), d.getEscalationDays(), d.isActive(), levelDtos,
                d.getCreatedAt());
    }
}
