package com.pcc.property.service;

import com.pcc.property.dto.request.MaintenanceRequestDto;
import com.pcc.property.dto.request.MaintenanceUpdateRequest;
import com.pcc.property.dto.response.HistoryEntryResponse;
import com.pcc.property.dto.response.MaintenanceResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.entity.*;
import com.pcc.property.enums.MaintenancePriority;
import com.pcc.property.enums.MaintenanceStatus;
import com.pcc.property.exception.BusinessRuleException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.*;
import com.pcc.property.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.pcc.property.service.PropertyService.actor;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRequestRepository requestRepository;
    private final MaintenanceHistoryRepository historyRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;

    @Transactional
    public MaintenanceResponse create(MaintenanceRequestDto req) {
        if (req.apartmentId() == null && req.buildingId() == null) {
            throw new BusinessRuleException("A maintenance request needs an apartment or a building");
        }
        MaintenanceRequest m = new MaintenanceRequest();
        if (req.apartmentId() != null) {
            Apartment apt = apartmentRepository.findById(req.apartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + req.apartmentId()));
            m.setApartment(apt);
            m.setBuilding(apt.getBuilding());
        } else {
            m.setBuilding(buildingRepository.findById(req.buildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + req.buildingId())));
        }
        m.setRequestNumber(nextNumber());
        m.setCategory(req.category());
        m.setDescription(req.description());
        if (req.priority() != null) m.setPriority(req.priority());
        m.setStatus(MaintenanceStatus.OPEN);
        UserPrincipal actor = actor();
        m.setCreatedBy(actor != null ? actor.getId() : null);
        m.setCreatedByName(actor != null ? actor.getUsername() : null);
        MaintenanceRequest saved = requestRepository.save(m);
        record(saved, "CREATED", "Request opened", actor);
        return MaintenanceResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse get(Long id) {
        return MaintenanceResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceResponse> search(Long apartmentId, Long buildingId,
                                                    MaintenanceStatus status, int page, int size) {
        return PageResponse.of(
                requestRepository.search(apartmentId, buildingId, status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                MaintenanceResponse::of);
    }

    @Transactional
    public MaintenanceResponse update(Long id, MaintenanceUpdateRequest req) {
        MaintenanceRequest m = find(id);
        UserPrincipal actor = actor();
        if (req.status() != null && req.status() != m.getStatus()) {
            MaintenanceStatus from = m.getStatus();
            m.setStatus(req.status());
            if (req.status() == MaintenanceStatus.COMPLETED) {
                m.setCompletedAt(Instant.now());
            } else {
                m.setCompletedAt(null);
            }
            record(m, "STATUS_" + req.status(), from + " → " + req.status(), actor);
        }
        if (req.assignedEmployeeId() != null) {
            m.setAssignedEmployeeId(req.assignedEmployeeId());
            m.setAssignedEmployeeName(req.assignedEmployeeName());
            record(m, "ASSIGNED", "Assigned to " + req.assignedEmployeeName(), actor);
        }
        if (req.note() != null && !req.note().isBlank()) {
            record(m, "NOTE", req.note(), actor);
        }
        return MaintenanceResponse.of(requestRepository.save(m));
    }

    @Transactional(readOnly = true)
    public List<HistoryEntryResponse> history(Long requestId) {
        find(requestId);
        return historyRepository.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                .map(h -> new HistoryEntryResponse(h.getEvent(), null, null,
                        h.getNote(), h.getActorUsername(), h.getCreatedAt()))
                .toList();
    }

    private MaintenanceRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + id));
    }

    private void record(MaintenanceRequest m, String event, String note, UserPrincipal actor) {
        MaintenanceHistory h = new MaintenanceHistory();
        h.setRequest(m);
        h.setEvent(event);
        h.setNote(note);
        h.setActorId(actor != null ? actor.getId() : null);
        h.setActorUsername(actor != null ? actor.getUsername() : null);
        historyRepository.save(h);
    }

    private String nextNumber() {
        String number;
        do {
            number = String.format("MNT-%04d", requestRepository.maxRequestSequence() + 1);
        } while (requestRepository.existsByRequestNumber(number));
        return number;
    }
}
