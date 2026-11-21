package com.pcc.finance.service;

import com.pcc.finance.entity.AuditLog;
import com.pcc.finance.entity.FinanceAccount;
import com.pcc.finance.entity.PaymentMethod;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.AuditLogRepository;
import com.pcc.finance.repository.FinanceAccountRepository;
import com.pcc.finance.repository.PaymentMethodRepository;
import com.pcc.finance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class FinanceSupport {

    private final PaymentMethodRepository methodRepository;
    private final FinanceAccountRepository accountRepository;
    private final AuditLogRepository auditRepository;

    public UserPrincipal actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p : null;
    }

    public String actorName() {
        UserPrincipal a = actor();
        return a != null ? a.getUsername() : "system";
    }

    public Long actorId() {
        UserPrincipal a = actor();
        return a != null ? a.getId() : null;
    }

    public PaymentMethod method(String code) {
        return methodRepository.findByCode(code.toUpperCase())
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + code));
    }

    public FinanceAccount account(Long id) {
        if (id == null) return null;
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found"));
    }

    /** Human ref from the entity id: PAY-0007. Guaranteed unique by the PK. */
    public static String ref(String prefix, Long id) {
        return String.format("%s%04d", prefix, id);
    }

    public void audit(String entityType, Long entityId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        log.setActorId(actorId());
        log.setActorName(actorName());
        auditRepository.save(log);
    }

    /** Retry wrapper for unique-ref races: the ref depends on id so this is a no-op helper kept for clarity. */
    public <T> T once(Supplier<T> op) {
        return op.get();
    }
}
