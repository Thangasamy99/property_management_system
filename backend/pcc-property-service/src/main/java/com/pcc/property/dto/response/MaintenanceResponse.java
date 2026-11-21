package com.pcc.property.dto.response;

import com.pcc.property.entity.MaintenanceRequest;

import java.time.Instant;

public record MaintenanceResponse(
        Long id, String requestNumber,
        Long apartmentId, String apartmentNumber,
        Long buildingId, String buildingName, String propertyName,
        String category, String description, String priority, String status,
        Long assignedEmployeeId, String assignedEmployeeName,
        Instant completedAt, String createdByName, Instant createdAt
) {
    public static MaintenanceResponse of(MaintenanceRequest m) {
        var apt = m.getApartment();
        var bld = m.getBuilding();
        String propertyName = bld != null ? bld.getProperty().getName()
                : (apt != null ? apt.getBuilding().getProperty().getName() : null);
        return new MaintenanceResponse(m.getId(), m.getRequestNumber(),
                apt != null ? apt.getId() : null, apt != null ? apt.getApartmentNumber() : null,
                bld != null ? bld.getId() : (apt != null ? apt.getBuilding().getId() : null),
                bld != null ? bld.getName() : (apt != null ? apt.getBuilding().getName() : null),
                propertyName,
                m.getCategory().name(), m.getDescription(), m.getPriority().name(), m.getStatus().name(),
                m.getAssignedEmployeeId(), m.getAssignedEmployeeName(),
                m.getCompletedAt(), m.getCreatedByName(), m.getCreatedAt());
    }
}
