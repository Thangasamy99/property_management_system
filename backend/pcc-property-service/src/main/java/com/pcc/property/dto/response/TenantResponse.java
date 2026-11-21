package com.pcc.property.dto.response;

import com.pcc.property.entity.Tenant;

import java.time.Instant;

public record TenantResponse(
        Long id, String code, String tenantType, String name, String email, String phone,
        String address, String identificationType, String identificationNumber,
        String emergencyContactName, String emergencyContactPhone,
        String status, Instant createdAt
) {
    public static TenantResponse of(Tenant t) {
        return new TenantResponse(t.getId(), t.getCode(), t.getTenantType().name(), t.getName(),
                t.getEmail(), t.getPhone(), t.getAddress(), t.getIdentificationType(),
                t.getIdentificationNumber(), t.getEmergencyContactName(), t.getEmergencyContactPhone(),
                t.getStatus().name(), t.getCreatedAt());
    }
}
