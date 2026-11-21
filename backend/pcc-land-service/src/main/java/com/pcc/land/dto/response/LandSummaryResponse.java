package com.pcc.land.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record LandSummaryResponse(
        long totalParcels,
        BigDecimal totalAreaSqm,
        Map<String, Long> parcelsByStatus,
        long totalSales,
        long pendingTransfers
) {}
