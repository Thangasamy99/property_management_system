package com.pcc.identity.audit;

import com.pcc.identity.dto.response.AuditLogResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.entity.AuditLog;
import com.pcc.identity.repository.AuditLogRepository;
import com.pcc.identity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Central audit-trail writer for security-relevant actions. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActorId(SecurityUtils.currentUserId());
        entry.setActorUsername(SecurityUtils.currentUsername());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action, String entityType, Pageable pageable) {
        return PageResponse.from(
                auditLogRepository.search(blankToNull(action), blankToNull(entityType), pageable),
                AuditService::toResponse);
    }

    private static AuditLogResponse toResponse(AuditLog e) {
        return new AuditLogResponse(e.getId(), e.getActorUsername(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getDetails(), e.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
