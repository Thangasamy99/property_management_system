package com.pcc.property.dto.request;

import com.pcc.property.enums.MaintenanceStatus;
import jakarta.validation.constraints.Size;

public record MaintenanceUpdateRequest(
        MaintenanceStatus status,
        Long assignedEmployeeId,
        @Size(max = 120) String assignedEmployeeName,
        @Size(max = 500) String note
) {}
