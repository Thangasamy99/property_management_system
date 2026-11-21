package com.pcc.land.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SaleDecisionRequest(String note, @Positive BigDecimal agreedPrice) {}
