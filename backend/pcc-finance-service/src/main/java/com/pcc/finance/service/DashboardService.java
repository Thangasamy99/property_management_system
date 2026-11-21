package com.pcc.finance.service;

import com.pcc.finance.dto.response.DashboardResponse;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import com.pcc.finance.enums.TargetType;
import com.pcc.finance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<PaymentType> COLLECTION_TYPES =
            List.of(PaymentType.RENT, PaymentType.LAND_ACQUISITION, PaymentType.LAND_SALE, PaymentType.OTHER_INCOME);

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentTargetRepository targetRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final LedgerEntryRepository ledgerRepository;

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        BigDecimal collectionsToday = paymentRepository.sumPosted(COLLECTION_TYPES, today, today);
        BigDecimal collectionsMonth = paymentRepository.sumPosted(COLLECTION_TYPES, monthStart, today);
        BigDecimal expensesToday = expenseRepository.sumPaidBetween(today, today);
        BigDecimal expensesMonth = expenseRepository.sumPaidBetween(monthStart, today);

        // Outstanding rent across all tracked rent-schedule targets.
        BigDecimal outstandingRent = targetRepository.findAll().stream()
                .filter(t -> t.getTargetType() == TargetType.RENT_SCHEDULE)
                .map(t -> t.getTotalAmount().subtract(netPaid(t.getId())))
                .filter(a -> a.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal landAcqPaid = paymentRepository.sumPosted(List.of(PaymentType.LAND_ACQUISITION),
                LocalDate.of(2000, 1, 1), today);
        BigDecimal landSalePaid = paymentRepository.sumPosted(List.of(PaymentType.LAND_SALE),
                LocalDate.of(2000, 1, 1), today);

        // Monthly income vs expense for the last 6 months.
        List<DashboardResponse.MonthlyPoint> series = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDate s = ym.atDay(1);
            LocalDate e = ym.atEndOfMonth().isAfter(today) ? today : ym.atEndOfMonth();
            series.add(new DashboardResponse.MonthlyPoint(
                    ym.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    paymentRepository.sumPosted(COLLECTION_TYPES, s, e),
                    expenseRepository.sumPaidBetween(s, e)));
        }

        List<DashboardResponse.RecentItem> recent = paymentRepository
                .findTop10ByStatusOrderByCreatedAtDesc(PaymentStatus.POSTED).stream()
                .map(p -> new DashboardResponse.RecentItem(
                        p.getPaymentRef(), p.getPaymentType().name(), p.getPayerName(),
                        p.getAmount(), p.getPaymentMethod().getCode(),
                        p.getStatus().name(), p.getPaymentDate()))
                .toList();

        return new DashboardResponse(collectionsToday, collectionsMonth,
                expensesToday, expensesMonth, outstandingRent,
                landAcqPaid, landSalePaid,
                ledgerRepository.cashBalance(), ledgerRepository.bankBalance(),
                series, recent);
    }

    private BigDecimal netPaid(Long targetId) {
        return allocationRepository.sumPostedNetForTarget(targetId);
    }
}
