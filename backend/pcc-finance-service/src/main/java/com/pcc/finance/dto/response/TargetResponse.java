package com.pcc.finance.dto.response;

import java.math.BigDecimal;

public record TargetResponse(
        Long id, String targetType, Long externalId,
        String externalReference, String parentReference, String description,
        BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal outstanding,
        boolean fullyPaid
) {}
