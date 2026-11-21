package com.pcc.property.service;

import com.pcc.property.dto.request.BuildingRequest;
import com.pcc.property.dto.response.BuildingResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.entity.Building;
import com.pcc.property.entity.Property;
import com.pcc.property.enums.BuildingStatus;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.ApartmentRepository;
import com.pcc.property.repository.BuildingRepository;
import com.pcc.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public BuildingResponse create(BuildingRequest req) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + req.propertyId()));
        Building b = new Building();
        apply(b, req);
        b.setProperty(property);
        b.setCode(nextCode());
        return BuildingResponse.of(buildingRepository.save(b), 0L);
    }

    @Transactional(readOnly = true)
    public BuildingResponse get(Long id) {
        Building b = find(id);
        return BuildingResponse.of(b, (long) apartmentRepository.findByBuildingId(id).size());
    }

    @Transactional(readOnly = true)
    public PageResponse<BuildingResponse> search(Long propertyId, String search, BuildingStatus status, int page, int size) {
        return PageResponse.of(
                buildingRepository.search(propertyId, blankToNull(search), status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                b -> BuildingResponse.of(b, (long) apartmentRepository.findByBuildingId(b.getId()).size()));
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> byProperty(Long propertyId) {
        return buildingRepository.findByPropertyId(propertyId).stream()
                .map(b -> BuildingResponse.of(b, (long) apartmentRepository.findByBuildingId(b.getId()).size()))
                .toList();
    }

    @Transactional
    public BuildingResponse update(Long id, BuildingRequest req) {
        Building b = find(id);
        apply(b, req);
        if (!b.getProperty().getId().equals(req.propertyId())) {
            b.setProperty(propertyRepository.findByIdAndDeletedAtIsNull(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + req.propertyId())));
        }
        return BuildingResponse.of(buildingRepository.save(b),
                (long) apartmentRepository.findByBuildingId(id).size());
    }

    private Building find(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + id));
    }

    private void apply(Building b, BuildingRequest req) {
        b.setName(req.name());
        b.setNumberOfFloors(req.numberOfFloors());
        b.setConstructionYear(req.constructionYear());
        b.setDescription(req.description());
        if (req.status() != null) b.setStatus(req.status());
    }

    private String nextCode() {
        String code;
        do {
            code = String.format("BLD-%04d", buildingRepository.maxBuildingCodeSequence() + 1);
        } while (buildingRepository.existsByCode(code));
        return code;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
