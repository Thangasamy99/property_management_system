package com.pcc.land.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum MeasurementUnit {
    SQUARE_METRE, HECTARE, ACRE;

    private static final BigDecimal HECTARE_SQM = new BigDecimal("10000");
    private static final BigDecimal ACRE_SQM = new BigDecimal("4046.8564224");

    public BigDecimal toSqm(BigDecimal value) {
        return (switch (this) {
            case SQUARE_METRE -> value;
            case HECTARE -> value.multiply(HECTARE_SQM);
            case ACRE -> value.multiply(ACRE_SQM);
        }).setScale(3, RoundingMode.HALF_UP);
    }
}
