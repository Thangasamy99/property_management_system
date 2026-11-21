package com.pcc.land.controller;

import com.pcc.land.dto.request.ParcelRequest;
import com.pcc.land.dto.request.ParcelStatusRequest;
import com.pcc.land.dto.response.*;
import com.pcc.land.enums.LandClassification;
import com.pcc.land.enums.LandStatus;
import com.pcc.land.service.LandParcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/land-parcels")
@RequiredArgsConstructor
@Tag(name = "Land Parcels")
@SecurityRequirement(name = "bearerAuth")
public class LandParcelController {

    private final LandParcelService parcelService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    @Operation(summary = "List land parcels")
    public PageResponse<ParcelResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LandStatus status,
            @RequestParam(required = false) LandClassification classification,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return parcelService.list(search, status, classification, region, page, size, sort);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('LAND_REPORT_VIEW') or hasAuthority('LAND_VIEW')")
    @Operation(summary = "Land dashboard summary")
    public LandSummaryResponse summary() {
        return parcelService.summary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public ParcelResponse get(@PathVariable Long id) {
        return parcelService.get(id);
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public List<StatusHistoryResponse> statusHistory(@PathVariable Long id) {
        return parcelService.statusHistory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_CREATE')")
    public ParcelResponse register(@Valid @RequestBody ParcelRequest request) {
        return parcelService.register(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_UPDATE')")
    public ParcelResponse update(@PathVariable Long id, @Valid @RequestBody ParcelRequest request) {
        return parcelService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('LAND_UPDATE')")
    public ParcelResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ParcelStatusRequest request) {
        return parcelService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LAND_DELETE')")
    public void delete(@PathVariable Long id) {
        parcelService.delete(id);
    }
}
