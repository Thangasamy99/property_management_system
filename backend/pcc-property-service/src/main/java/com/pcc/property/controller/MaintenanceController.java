package com.pcc.property.controller;

import com.pcc.property.dto.request.MaintenanceRequestDto;
import com.pcc.property.dto.request.MaintenanceUpdateRequest;
import com.pcc.property.dto.response.HistoryEntryResponse;
import com.pcc.property.dto.response.MaintenanceResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.enums.MaintenanceStatus;
import com.pcc.property.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public PageResponse<MaintenanceResponse> list(
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(apartmentId, buildingId, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public MaintenanceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public List<HistoryEntryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    public MaintenanceResponse create(@Valid @RequestBody MaintenanceRequestDto req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    public MaintenanceResponse update(@PathVariable Long id, @Valid @RequestBody MaintenanceUpdateRequest req) {
        return service.update(id, req);
    }
}
