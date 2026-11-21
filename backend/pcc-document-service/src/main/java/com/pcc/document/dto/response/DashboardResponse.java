package com.pcc.document.dto.response;

public record DashboardResponse(long activeDocuments, long archivedDocuments,
                                ExpirySummaryResponse expiry) {}
