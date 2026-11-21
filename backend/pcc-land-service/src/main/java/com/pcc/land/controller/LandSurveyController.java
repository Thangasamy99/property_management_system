package com.pcc.land.controller;

import com.pcc.land.dto.request.SurveyRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.SurveyResponse;
import com.pcc.land.service.LandSurveyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/land-surveys")
@RequiredArgsConstructor
@Tag(name = "Land Surveys")
@SecurityRequirement(name = "bearerAuth")
public class LandSurveyController {

    private final LandSurveyService surveyService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_SURVEY_VIEW') or hasAuthority('LAND_VIEW')")
    public PageResponse<SurveyResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return surveyService.list(parcelId, search, page, size);
    }

    @GetMapping("/by-parcel/{parcelId}")
    @PreAuthorize("hasAuthority('LAND_SURVEY_VIEW') or hasAuthority('LAND_VIEW')")
    public List<SurveyResponse> byParcel(@PathVariable Long parcelId) {
        return surveyService.byParcel(parcelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_SURVEY_CREATE')")
    public SurveyResponse create(@Valid @RequestBody SurveyRequest request) {
        return surveyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LAND_SURVEY_UPDATE')")
    public SurveyResponse update(@PathVariable Long id, @Valid @RequestBody SurveyRequest request) {
        return surveyService.update(id, request);
    }
}
