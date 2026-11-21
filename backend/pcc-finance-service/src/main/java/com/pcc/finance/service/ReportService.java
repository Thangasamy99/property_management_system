package com.pcc.finance.service;

import com.pcc.finance.dto.response.*;
import com.pcc.finance.enums.ExpenseStatus;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import com.pcc.finance.enums.TargetType;
import com.pcc.finance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final List<PaymentType> COLLECTION_TYPES =
            List.of(PaymentType.RENT, PaymentType.LAND_ACQUISITION, PaymentType.LAND_SALE, PaymentType.OTHER_INCOME);

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final PaymentTargetRepository targetRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final FinanceAccountRepository accountRepository;
    private final PaymentService paymentService;

    /** Daily or arbitrary-range collection report. */
    @Transactional(readOnly = true)
    public Map<String, Object> collection(LocalDate from, LocalDate to) {
        PageResponse<PaymentResponse> payments = paymentService.list(
                null, PaymentStatus.POSTED, from, to, null, 0, 500);
        Map<String, BigDecimal> byMethod = new TreeMap<>();
        Map<String, BigDecimal> byType = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentResponse p : payments.content()) {
            if (p.paymentType().equals(PaymentType.EXPENSE_PAYOUT.name())) continue;
            total = total.add(p.amount());
            byMethod.merge(p.methodCode(), p.amount(), BigDecimal::add);
            byType.merge(p.paymentType(), p.amount(), BigDecimal::add);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", from);
        r.put("to", to);
        r.put("total", total);
        r.put("count", payments.content().size());
        r.put("byMethod", byMethod);
        r.put("byType", byType);
        r.put("payments", payments.content());
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> monthlyCollection(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return collection(ym.atDay(1), ym.atEndOfMonth());
    }

    /** Rent arrears: rent targets whose outstanding balance is still positive. */
    @Transactional(readOnly = true)
    public Map<String, Object> rentArrears() {
        List<TargetResponse> open = paymentService.openTargets(TargetType.RENT_SCHEDULE);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("outstandingTotal", open.stream().map(TargetResponse::outstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        r.put("count", open.size());
        r.put("targets", open);
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> expenses(LocalDate from, LocalDate to, String category, int page, int size) {
        var result = expenseRepository.search(null, category, from, to, null,
                PageRequest.of(page, size));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", expenseRepository.sumPaidBetween(from, to));
        r.put("byCategory", expenseRepository.sumPaidByCategory(from, to));
        r.put("expenses", PageResponse.of(result, ExpenseResponse::of));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> incomeVsExpense(LocalDate from, LocalDate to) {
        Map<String, Object> r = new LinkedHashMap<>();
        BigDecimal income = paymentRepository.sumPosted(COLLECTION_TYPES, from, to);
        BigDecimal expense = expenseRepository.sumPaidBetween(from, to);
        r.put("from", from);
        r.put("to", to);
        r.put("income", income);
        r.put("expenses", expense);
        r.put("net", income.subtract(expense));
        r.put("incomeByType", paymentRepository.sumPostedByType(from, to));
        r.put("expenseByCategory", expenseRepository.sumPaidByCategory(from, to));
        return r;
    }

    /** Cashbook for one account: opening balance + posted movements + closing. */
    @Transactional(readOnly = true)
    public Map<String, Object> cashbook(Long accountId, LocalDate from, LocalDate to, int page, int size) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new com.pcc.finance.exception.ResourceNotFoundException("Account not found"));
        var entries = ledgerRepository.search(null, null, accountId, from, to, null,
                PageRequest.of(page, size));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("account", AccountResponse.of(account, ledgerRepository.balanceForAccount(accountId)));
        r.put("openingBalance", account.getOpeningBalance());
        r.put("entries", PageResponse.of(entries, LedgerEntryResponse::of));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> paymentMethodSummary(LocalDate from, LocalDate to) {
        PageResponse<PaymentResponse> payments = paymentService.list(
                null, PaymentStatus.POSTED, from, to, null, 0, 1000);
        Map<String, BigDecimal> byMethod = new TreeMap<>();
        Map<String, Long> countByMethod = new TreeMap<>();
        for (PaymentResponse p : payments.content()) {
            if (p.paymentType().equals(PaymentType.EXPENSE_PAYOUT.name())) continue;
            byMethod.merge(p.methodCode(), p.amount(), BigDecimal::add);
            countByMethod.merge(p.methodCode(), 1L, Long::sum);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", from);
        r.put("to", to);
        r.put("amountsByMethod", byMethod);
        r.put("countByMethod", countByMethod);
        return r;
    }
}
