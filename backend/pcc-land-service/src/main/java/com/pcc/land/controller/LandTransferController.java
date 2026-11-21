package com.pcc.land.controller;

import com.pcc.land.dto.request.DecisionRequest;
import com.pcc.land.dto.request.TransferCompleteRequest;
import com.pcc.land.dto.request.TransferRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.TransferResponse;
import com.pcc.land.enums.TransferStatus;
import com.pcc.land.service.LandTransferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/land-transfers")
@RequiredArgsConstructor
@Tag(name = "Land Transfers")
@SecurityRequirement(name = "bearerAuth")
public class LandTransferController {

    private final LandTransferService transferService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public PageResponse<TransferResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.list(parcelId, status, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public TransferResponse get(@PathVariable Long id) {
        return transferService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public TransferResponse create(@Valid @RequestBody TransferRequest request) {
        return transferService.create(request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public TransferResponse submit(@PathVariable Long id) {
        return transferService.submit(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public TransferResponse approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return transferService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public TransferResponse reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return transferService.reject(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public TransferResponse complete(@PathVariable Long id, @Valid @RequestBody TransferCompleteRequest request) {
        return transferService.complete(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public TransferResponse cancel(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return transferService.cancel(id, request);
    }
}
