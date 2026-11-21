package com.pcc.property.service;

import com.pcc.property.dto.request.PropertyRequest;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.PropertyResponse;
import com.pcc.property.entity.Property;
import com.pcc.property.entity.PropertyAddress;
import com.pcc.property.entity.PropertyStatusHistory;
import com.pcc.property.enums.PropertyStatus;
import com.pcc.property.enums.PropertyType;
import com.pcc.property.exception.DuplicateResourceException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.BuildingRepository;
import com.pcc.property.repository.PropertyRepository;
import com.pcc.property.repository.PropertyStatusHistoryRepository;
import com.pcc.property.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public PropertyResponse create(PropertyRequest req) {
        Property p = new Property();
        apply(p, req);
        p.setStatus(PropertyStatus.ACTIVE);
        p.setCode(nextCode());
        UserPrincipal actor = actor();
        p.setCreatedBy(actor != null ? actor.getId() : null);
        p.setCreatedByName(actor != null ? actor.getUsername() : null);
        Property saved = propertyRepository.save(p);
        recordStatus(saved, null, PropertyStatus.ACTIVE, "Property registered", actor);
        return PropertyResponse.of(saved, 0L);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        Property p = find(id);
        return PropertyResponse.of(p, (long) buildingRepository.findByPropertyId(id).size());
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> search(String search, PropertyStatus status, PropertyType type, int page, int size) {
        return PageResponse.of(
                propertyRepository.search(blankToNull(search), status, type,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                PropertyResponse::of);
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyRequest req) {
        Property p = find(id);
        apply(p, req);
        return PropertyResponse.of(propertyRepository.save(p),
                (long) buildingRepository.findByPropertyId(id).size());
    }

    @Transactional
    public PropertyResponse changeStatus(Long id, PropertyStatus status, String reason) {
        Property p = find(id);
        PropertyStatus from = p.getStatus();
        p.setStatus(status);
        Property saved = propertyRepository.save(p);
        recordStatus(saved, from, status, reason, actor());
        return PropertyResponse.of(saved);
    }

    @Transactional
    public void delete(Long id) {
        Property p = find(id);
        p.setDeletedAt(Instant.now());
        p.setStatus(PropertyStatus.INACTIVE);
        propertyRepository.save(p);
        recordStatus(p, p.getStatus(), PropertyStatus.INACTIVE, "Property archived", actor());
    }

    private Property find(Long id) {
        return propertyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));
    }

    private void apply(Property p, PropertyRequest req) {
        p.setName(req.name());
        p.setPropertyType(req.propertyType());
        p.setDescription(req.description());
        p.setAcquisitionDate(req.acquisitionDate());
        p.setAcquisitionCost(req.acquisitionCost());
        if (hasAddress(req)) {
            PropertyAddress a = p.getAddress() != null ? p.getAddress() : new PropertyAddress();
            a.setLine1(req.addressLine1());
            a.setLine2(req.addressLine2());
            a.setCity(req.city());
            a.setRegion(req.region());
            if (req.country() != null && !req.country().isBlank()) a.setCountry(req.country());
            p.setAddress(a);
        }
    }

    private boolean hasAddress(PropertyRequest r) {
        return r.addressLine1() != null || r.addressLine2() != null || r.city() != null || r.region() != null;
    }

    private void recordStatus(Property p, PropertyStatus from, PropertyStatus to, String reason, UserPrincipal actor) {
        PropertyStatusHistory h = new PropertyStatusHistory();
        h.setProperty(p);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setReason(reason);
        h.setActorId(actor != null ? actor.getId() : null);
        h.setActorUsername(actor != null ? actor.getUsername() : null);
        statusHistoryRepository.save(h);
    }

    private String nextCode() {
        String code;
        do {
            code = String.format("PROP-%04d", propertyRepository.maxPropertyCodeSequence() + 1);
            // loop guards the race where another transaction inserts the same code
        } while (propertyRepository.existsByCode(code));
        return code;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    static UserPrincipal actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p : null;
    }

    static void assertUnique(boolean exists, String message) {
        if (exists) throw new DuplicateResourceException(message);
    }
}
