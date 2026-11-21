package com.pcc.document.dto.response;

public record ExpirySummaryResponse(long expired, long expiringWithin30, long expiringWithin60,
                                    long noExpiry) {}
