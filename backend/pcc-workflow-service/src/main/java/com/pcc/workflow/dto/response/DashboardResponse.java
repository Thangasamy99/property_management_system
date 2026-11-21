package com.pcc.workflow.dto.response;

public record DashboardResponse(long pendingMyApprovals, long mySubmitted, long submitted,
                                long pendingApproval, long approved, long rejected, long returned,
                                long completed, long overdue, long integrationFailed) {}
