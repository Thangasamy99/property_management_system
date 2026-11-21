package com.pcc.land.controller;

import com.pcc.land.dto.request.OwnershipRequest;
import com.pcc.land.dto.response.OwnershipHistoryResponse;
import com.pcc.land.dto.response.OwnershipResponse;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.enums.OwnershipStatus;
import com.pcc.land.service.LandOwnershipService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/land-ownership")
@RequiredArgsConstructor
@Tag(name = "Land Ownership")
@SecurityRequirement(name = "bearerAuth")
public class LandOwnershipController {

    private final LandOwnershipService ownershipService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_OWNERSHIP_VIEW') or hasAuthority('LAND_VIEW')")
    public PageResponse<OwnershipResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) OwnershipStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ownershipService.list(parcelId, status, search, page, size);
    }

    @GetMapping("/by-parcel/{parcelId}")
    @PreAuthorize("hasAuthority('LAND_OWNERSHIP_VIEW') or hasAuthority('LAND_VIEW')")
    public List<OwnershipResponse> byParcel(@PathVariable Long parcelId) {
        return ownershipService.byParcel(parcelId);
    }

    @GetMapping("/history/{parcelId}")
    @PreAuthorize("hasAuthority('LAND_OWNERSHIP_VIEW') or hasAuthority('LAND_VIEW')")
    public List<OwnershipHistoryResponse> history(@PathVariable Long parcelId) {
        return ownershipService.history(parcelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_OWNERSHIP_CREATE')")
    public OwnershipResponse register(@Valid @RequestBody OwnershipRequest request) {
        return ownershipService.register(request);
    }
}
