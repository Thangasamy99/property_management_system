package com.pcc.property.controller;

import com.pcc.property.dto.request.ApartmentRequest;
import com.pcc.property.dto.response.ApartmentResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.enums.ApartmentStatus;
import com.pcc.property.service.ApartmentService;
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
@RequestMapping("/api/v1/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('APARTMENT_VIEW')")
    public PageResponse<ApartmentResponse> list(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApartmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(buildingId, search, status, page, size);
    }

    @GetMapping("/by-building/{buildingId}")
    @PreAuthorize("hasAuthority('APARTMENT_VIEW')")
    public List<ApartmentResponse> byBuilding(@PathVariable Long buildingId) {
        return service.byBuilding(buildingId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APARTMENT_VIEW')")
    public ApartmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('APARTMENT_CREATE')")
    public ApartmentResponse create(@Valid @RequestBody ApartmentRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('APARTMENT_UPDATE')")
    public ApartmentResponse update(@PathVariable Long id, @Valid @RequestBody ApartmentRequest req) {
        return service.update(id, req);
    }
}
