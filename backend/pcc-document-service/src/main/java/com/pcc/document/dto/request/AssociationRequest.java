package com.pcc.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AssociationRequest(
        @NotBlank @Pattern(regexp = "PROPERTY|BUILDING|APARTMENT|RENTAL_AGREEMENT|TENANT|LAND_PARCEL|LAND_SURVEY|LAND_ACQUISITION|LAND_SALE|LAND_TRANSFER|FINANCE_PAYMENT|EXPENSE|RECEIPT|OTHER") String entityType,
        @NotBlank String entityRef) {}
