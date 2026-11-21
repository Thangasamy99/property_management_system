package com.pcc.finance.dto.request;

import jakarta.validation.constraints.Size;

public record DecisionRequest(@Size(max = 255) String note) {}
