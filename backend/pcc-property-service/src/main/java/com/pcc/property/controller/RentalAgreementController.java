package com.pcc.property.controller;

import com.pcc.property.dto.request.RenewAgreementRequest;
import com.pcc.property.dto.request.RentalAgreementRequest;
import com.pcc.property.dto.request.TerminateAgreementRequest;
import com.pcc.property.dto.response.HistoryEntryResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.RentScheduleResponse;
import com.pcc.property.dto.response.RentalAgreementResponse;
import com.pcc.property.enums.AgreementStatus;
import com.pcc.property.service.RentalService;
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
@RequestMapping("/api/v1/rental-agreements")
@RequiredArgsConstructor
public class RentalAgreementController {

    private final RentalService service;

    @GetMapping
    @PreAuthorize("hasAuthority('RENTAL_VIEW')")
    public PageResponse<RentalAgreementResponse> list(
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) AgreementStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(apartmentId, tenantId, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RENTAL_VIEW')")
    public RentalAgreementResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/schedules")
    @PreAuthorize("hasAuthority('RENTAL_VIEW')")
    public List<RentScheduleResponse> schedules(@PathVariable Long id) {
        return service.schedules(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('RENTAL_VIEW')")
    public List<HistoryEntryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RENTAL_MANAGE')")
    public RentalAgreementResponse create(@Valid @RequestBody RentalAgreementRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('RENTAL_MANAGE')")
    public RentalAgreementResponse renew(@PathVariable Long id, @Valid @RequestBody RenewAgreementRequest req) {
        return service.renew(id, req.newEndDate());
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('RENTAL_MANAGE')")
    public RentalAgreementResponse terminate(@PathVariable Long id, @Valid @RequestBody TerminateAgreementRequest req) {
        return service.terminate(id, req.reason());
    }
}
