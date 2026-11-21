package com.pcc.property.controller;

import com.pcc.property.dto.request.TenantRequest;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.TenantResponse;
import com.pcc.property.enums.TenantStatus;
import com.pcc.property.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService service;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    public PageResponse<TenantResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(search, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    public TenantResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public TenantResponse create(@Valid @RequestBody TenantRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantRequest req) {
        return service.update(id, req);
    }
}
