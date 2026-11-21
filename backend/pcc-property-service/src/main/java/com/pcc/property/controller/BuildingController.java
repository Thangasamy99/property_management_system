package com.pcc.property.controller;

import com.pcc.property.dto.request.BuildingRequest;
import com.pcc.property.dto.response.BuildingResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.enums.BuildingStatus;
import com.pcc.property.service.BuildingService;
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
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BUILDING_VIEW')")
    public PageResponse<BuildingResponse> list(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BuildingStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(propertyId, search, status, page, size);
    }

    @GetMapping("/by-property/{propertyId}")
    @PreAuthorize("hasAuthority('BUILDING_VIEW')")
    public List<BuildingResponse> byProperty(@PathVariable Long propertyId) {
        return service.byProperty(propertyId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BUILDING_VIEW')")
    public BuildingResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BUILDING_CREATE')")
    public BuildingResponse create(@Valid @RequestBody BuildingRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BUILDING_UPDATE')")
    public BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest req) {
        return service.update(id, req);
    }
}
