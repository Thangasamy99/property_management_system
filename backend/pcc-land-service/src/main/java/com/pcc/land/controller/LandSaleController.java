package com.pcc.land.controller;

import com.pcc.land.dto.request.SaleDecisionRequest;
import com.pcc.land.dto.request.SaleRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.SaleResponse;
import com.pcc.land.enums.SaleStatus;
import com.pcc.land.service.LandSaleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/land-sales")
@RequiredArgsConstructor
@Tag(name = "Land Sales")
@SecurityRequirement(name = "bearerAuth")
public class LandSaleController {

    private final LandSaleService saleService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return saleService.list(parcelId, status, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public SaleResponse create(@Valid @RequestBody SaleRequest request) {
        return saleService.create(request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public SaleResponse submit(@PathVariable Long id) {
        return saleService.submit(id);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public SaleResponse startReview(@PathVariable Long id) {
        return saleService.startReview(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public SaleResponse approve(@PathVariable Long id, @RequestBody(required = false) SaleDecisionRequest request) {
        return saleService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public SaleResponse reject(@PathVariable Long id, @RequestBody(required = false) SaleDecisionRequest request) {
        return saleService.reject(id, request);
    }

    @PatchMapping("/{id}/finance-status")
    @PreAuthorize("hasAuthority('LAND_PAYMENT_SYNC')")
    public SaleResponse financeStatus(@PathVariable Long id,
                                      @Valid @RequestBody com.pcc.land.dto.request.FinanceStatusRequest req) {
        return saleService.updateFinanceStatus(id, req.paidAmount());
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasAuthority('LAND_SALE_APPROVE')")
    public SaleResponse progress(@PathVariable Long id) {
        return saleService.progress(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('LAND_SALE_REQUEST')")
    public SaleResponse cancel(@PathVariable Long id, @RequestBody(required = false) SaleDecisionRequest request) {
        return saleService.cancel(id, request);
    }
}
