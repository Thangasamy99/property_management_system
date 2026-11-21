package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SurveyRequest(
        @NotNull Long parcelId,
        @NotNull LocalDate surveyDate,
        String surveyorName,
        String surveyorOrganization,
        @Positive BigDecimal surveyedArea,
        String boundaryNorth,
        String boundarySouth,
        String boundaryEast,
        String boundaryWest,
        String description,
        String documentReference
) {}
