package com.pcc.property.service;

import com.pcc.property.dto.request.ApartmentRequest;
import com.pcc.property.dto.response.ApartmentResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.entity.Apartment;
import com.pcc.property.entity.Building;
import com.pcc.property.enums.ApartmentStatus;
import com.pcc.property.exception.DuplicateResourceException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.ApartmentRepository;
import com.pcc.property.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;

    @Transactional
    public ApartmentResponse create(ApartmentRequest req) {
        Building building = buildingRepository.findById(req.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + req.buildingId()));
        if (apartmentRepository.existsByBuildingIdAndApartmentNumber(req.buildingId(), req.apartmentNumber())) {
            throw new DuplicateResourceException(
                    "Apartment " + req.apartmentNumber() + " already exists in this building");
        }
        Apartment a = new Apartment();
        apply(a, req);
        a.setBuilding(building);
        if (a.getStatus() == null) a.setStatus(ApartmentStatus.AVAILABLE);
        return ApartmentResponse.of(apartmentRepository.save(a));
    }

    @Transactional(readOnly = true)
    public ApartmentResponse get(Long id) {
        return ApartmentResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApartmentResponse> search(Long buildingId, String search, ApartmentStatus status, int page, int size) {
        return PageResponse.of(
                apartmentRepository.search(buildingId, blankToNull(search), status,
                        PageRequest.of(page, size, Sort.by("apartmentNumber"))),
                ApartmentResponse::of);
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> byBuilding(Long buildingId) {
        return apartmentRepository.findByBuildingId(buildingId).stream().map(ApartmentResponse::of).toList();
    }

    @Transactional
    public ApartmentResponse update(Long id, ApartmentRequest req) {
        Apartment a = find(id);
        if (!a.getBuilding().getId().equals(req.buildingId())) {
            Building building = buildingRepository.findById(req.buildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + req.buildingId()));
            a.setBuilding(building);
        }
        Long bId = a.getBuilding().getId();
        if (!a.getApartmentNumber().equals(req.apartmentNumber())
                && apartmentRepository.existsByBuildingIdAndApartmentNumber(bId, req.apartmentNumber())) {
            throw new DuplicateResourceException(
                    "Apartment " + req.apartmentNumber() + " already exists in this building");
        }
        apply(a, req);
        return ApartmentResponse.of(apartmentRepository.save(a));
    }

    private Apartment find(Long id) {
        return apartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + id));
    }

    private void apply(Apartment a, ApartmentRequest req) {
        a.setApartmentNumber(req.apartmentNumber());
        a.setFloorNumber(req.floorNumber());
        a.setBedrooms(req.bedrooms());
        a.setBathrooms(req.bathrooms());
        a.setAreaSqm(req.areaSqm());
        a.setMonthlyRent(req.monthlyRent());
        a.setDescription(req.description());
        if (req.status() != null) a.setStatus(req.status());
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
