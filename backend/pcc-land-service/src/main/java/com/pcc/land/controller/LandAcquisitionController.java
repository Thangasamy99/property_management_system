package com.pcc.land.controller;

import com.pcc.land.dto.request.AcquisitionRequest;
import com.pcc.land.dto.request.DecisionRequest;
import com.pcc.land.dto.response.AcquisitionResponse;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.enums.AcquisitionStatus;
import com.pcc.land.service.LandAcquisitionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/land-acquisitions")
@RequiredArgsConstructor
@Tag(name = "Land Acquisitions")
@SecurityRequirement(name = "bearerAuth")
public class LandAcquisitionController {

    private final LandAcquisitionService acquisitionService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public PageResponse<AcquisitionResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) AcquisitionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return acquisitionService.list(parcelId, status, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public AcquisitionResponse get(@PathVariable Long id) {
        return acquisitionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_ACQUISITION_CREATE') or hasAuthority('LAND_ACQUISITION_MANAGE')")
    public AcquisitionResponse create(@Valid @RequestBody AcquisitionRequest request) {
        return acquisitionService.create(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LAND_ACQUISITION_APPROVE')")
    public AcquisitionResponse approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return acquisitionService.approve(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('LAND_ACQUISITION_APPROVE')")
    public AcquisitionResponse complete(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return acquisitionService.complete(id, request);
    }

    @PatchMapping("/{id}/finance-status")
    @PreAuthorize("hasAuthority('LAND_PAYMENT_SYNC')")
    public AcquisitionResponse financeStatus(@PathVariable Long id,
                                             @Valid @RequestBody com.pcc.land.dto.request.FinanceStatusRequest req) {
        return acquisitionService.updateFinanceStatus(id, req.paidAmount());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('LAND_ACQUISITION_CREATE') or hasAuthority('LAND_ACQUISITION_MANAGE')")
    public AcquisitionResponse cancel(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return acquisitionService.cancel(id, request);
    }
}
