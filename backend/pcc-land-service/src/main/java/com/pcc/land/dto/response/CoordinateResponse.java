package com.pcc.land.dto.response;

import com.pcc.land.entity.LandCoordinate;

import java.math.BigDecimal;

public record CoordinateResponse(
        Long id,
        Integer sequence,
        BigDecimal latitude,
        BigDecimal longitude,
        String label
) {
    public static CoordinateResponse from(LandCoordinate c) {
        return new CoordinateResponse(c.getId(), c.getSequence(), c.getLatitude(), c.getLongitude(), c.getLabel());
    }
}
