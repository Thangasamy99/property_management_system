package com.pcc.property.dto.request;

import com.pcc.property.enums.BuildingStatus;
import jakarta.validation.constraints.*;

public record BuildingRequest(
        @NotNull Long propertyId,
        @NotBlank @Size(max = 150) String name,
        @Positive Integer numberOfFloors,
        @Min(1800) @Max(2100) Integer constructionYear,
        @Size(max = 4000) String description,
        BuildingStatus status
) {}
