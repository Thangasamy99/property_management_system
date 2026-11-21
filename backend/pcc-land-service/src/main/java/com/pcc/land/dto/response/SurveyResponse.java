package com.pcc.land.dto.response;

import com.pcc.land.entity.LandSurvey;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SurveyResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String surveyReference,
        LocalDate surveyDate,
        String surveyorName,
        String surveyorOrganization,
        BigDecimal surveyedArea,
        String boundaryNorth,
        String boundarySouth,
        String boundaryEast,
        String boundaryWest,
        String description,
        String documentReference,
        String createdByName,
        Instant createdAt
) {
    public static SurveyResponse from(LandSurvey s) {
        return new SurveyResponse(s.getId(), s.getParcel().getId(), s.getParcel().getParcelReference(),
                s.getParcel().getName(), s.getSurveyReference(), s.getSurveyDate(), s.getSurveyorName(),
                s.getSurveyorOrganization(), s.getSurveyedArea(), s.getBoundaryNorth(), s.getBoundarySouth(),
                s.getBoundaryEast(), s.getBoundaryWest(), s.getDescription(), s.getDocumentReference(),
                s.getCreatedByName(), s.getCreatedAt());
    }
}
