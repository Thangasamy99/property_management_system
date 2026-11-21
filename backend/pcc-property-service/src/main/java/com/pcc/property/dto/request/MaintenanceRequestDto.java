package com.pcc.property.dto.request;

import com.pcc.property.enums.MaintenanceCategory;
import com.pcc.property.enums.MaintenancePriority;
import jakarta.validation.constraints.*;

public record MaintenanceRequestDto(
        Long apartmentId,
        Long buildingId,
        @NotNull MaintenanceCategory category,
        @NotBlank @Size(max = 4000) String description,
        MaintenancePriority priority
) {}
