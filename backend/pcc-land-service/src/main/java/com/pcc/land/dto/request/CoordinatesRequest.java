package com.pcc.land.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CoordinatesRequest(
        @NotNull Long parcelId,
        @NotEmpty List<@Valid CoordinatePointRequest> points
) {}
