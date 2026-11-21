package com.pcc.finance.dto.request;

import jakarta.validation.constraints.Size;

/** Confirm/post a RECORDED payment after verification evidence is available. */
public record PostPaymentRequest(
        @Size(max = 255) String verificationDetail,
        Long accountId
) {}
