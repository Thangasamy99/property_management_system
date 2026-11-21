package com.pcc.property.dto.request;

import com.pcc.property.enums.TenantStatus;
import com.pcc.property.enums.TenantType;
import jakarta.validation.constraints.*;

public record TenantRequest(
        @NotNull TenantType tenantType,
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 40) String phone,
        @Size(max = 255) String address,
        @Size(max = 40) String identificationType,
        @Size(max = 80) String identificationNumber,
        @Size(max = 120) String emergencyContactName,
        @Size(max = 40) String emergencyContactPhone,
        TenantStatus status
) {}
