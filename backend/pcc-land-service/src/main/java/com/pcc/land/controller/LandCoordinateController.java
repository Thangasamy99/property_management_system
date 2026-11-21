package com.pcc.land.controller;

import com.pcc.land.dto.request.CoordinatesRequest;
import com.pcc.land.dto.response.CoordinateResponse;
import com.pcc.land.service.LandCoordinateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/land-coordinates")
@RequiredArgsConstructor
@Tag(name = "Land Coordinates")
@SecurityRequirement(name = "bearerAuth")
public class LandCoordinateController {

    private final LandCoordinateService coordinateService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public List<CoordinateResponse> byParcel(@RequestParam Long parcelId) {
        return coordinateService.byParcel(parcelId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('LAND_UPDATE')")
    public List<CoordinateResponse> replace(@Valid @RequestBody CoordinatesRequest request) {
        return coordinateService.replace(request);
    }
}
