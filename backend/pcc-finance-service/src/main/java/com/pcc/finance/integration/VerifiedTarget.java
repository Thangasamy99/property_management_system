package com.pcc.finance.integration;

import java.math.BigDecimal;

/** Verified details of an external payable object, fetched from its owning service. */
public record VerifiedTarget(
        Long externalId,
        String externalReference,
        String parentReference,
        String description,
        String payerHint,
        BigDecimal totalAmount,
        String snapshotJson
) {}
