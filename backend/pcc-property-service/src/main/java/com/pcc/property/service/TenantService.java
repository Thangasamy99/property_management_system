package com.pcc.property.service;

import com.pcc.property.dto.request.TenantRequest;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.TenantResponse;
import com.pcc.property.entity.Tenant;
import com.pcc.property.enums.TenantStatus;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse create(TenantRequest req) {
        Tenant t = new Tenant();
        apply(t, req);
        t.setCode(nextCode());
        return TenantResponse.of(tenantRepository.save(t));
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return TenantResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> search(String search, TenantStatus status, int page, int size) {
        return PageResponse.of(
                tenantRepository.search(blankToNull(search), status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                TenantResponse::of);
    }

    @Transactional
    public TenantResponse update(Long id, TenantRequest req) {
        Tenant t = find(id);
        apply(t, req);
        return TenantResponse.of(tenantRepository.save(t));
    }

    private Tenant find(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    private void apply(Tenant t, TenantRequest req) {
        t.setTenantType(req.tenantType());
        t.setName(req.name());
        t.setEmail(req.email());
        t.setPhone(req.phone());
        t.setAddress(req.address());
        t.setIdentificationType(req.identificationType());
        t.setIdentificationNumber(req.identificationNumber());
        t.setEmergencyContactName(req.emergencyContactName());
        t.setEmergencyContactPhone(req.emergencyContactPhone());
        if (req.status() != null) t.setStatus(req.status());
    }

    private String nextCode() {
        String code;
        do {
            code = String.format("TNT-%04d", tenantRepository.maxTenantCodeSequence() + 1);
        } while (tenantRepository.existsByCode(code));
        return code;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
