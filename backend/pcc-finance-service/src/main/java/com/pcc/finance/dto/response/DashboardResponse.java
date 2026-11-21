package com.pcc.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        BigDecimal collectionsToday,
        BigDecimal collectionsThisMonth,
        BigDecimal expensesToday,
        BigDecimal expensesThisMonth,
        BigDecimal outstandingRent,
        BigDecimal landAcquisitionPaid,
        BigDecimal landSaleReceipts,
        BigDecimal cashBalance,
        BigDecimal bankBalance,
        List<MonthlyPoint> monthlySeries,
        List<RecentItem> recentTransactions
) {
    public record MonthlyPoint(String month, BigDecimal income, BigDecimal expenses) {}

    public record RecentItem(String ref, String type, String payer, BigDecimal amount,
                             String method, String status, LocalDate date) {}
}
