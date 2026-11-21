package com.pcc.property.controller;

import com.pcc.property.dto.request.PropertyRequest;
import com.pcc.property.dto.request.PropertyStatusRequest;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.PropertyResponse;
import com.pcc.property.enums.PropertyStatus;
import com.pcc.property.enums.PropertyType;
import com.pcc.property.service.PropertyService;
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
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PROPERTY_VIEW')")
    public PageResponse<PropertyResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.search(search, status, type, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROPERTY_VIEW')")
    public PropertyResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROPERTY_CREATE')")
    public PropertyResponse create(@Valid @RequestBody PropertyRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROPERTY_UPDATE')")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PROPERTY_UPDATE')")
    public PropertyResponse changeStatus(@PathVariable Long id, @Valid @RequestBody PropertyStatusRequest req) {
        return service.changeStatus(id, req.status(), req.reason());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROPERTY_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
