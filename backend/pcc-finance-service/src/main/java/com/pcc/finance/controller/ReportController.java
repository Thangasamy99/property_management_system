package com.pcc.finance.controller;

import com.pcc.finance.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Finance Reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('FINANCIAL_REPORT_VIEW')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily-collection")
    public Map<String, Object> dailyCollection(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date == null ? LocalDate.now() : date;
        return reportService.collection(d, d);
    }

    @GetMapping("/monthly-collection")
    public Map<String, Object> monthlyCollection(@RequestParam int year, @RequestParam int month) {
        return reportService.monthlyCollection(year, month);
    }

    @GetMapping("/collections")
    public Map<String, Object> collections(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.collection(from, to);
    }

    @GetMapping("/rent-arrears")
    public Map<String, Object> rentArrears() {
        return reportService.rentArrears();
    }

    @GetMapping("/expenses")
    public Map<String, Object> expenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reportService.expenses(from, to, category, page, size);
    }

    @GetMapping("/income-vs-expense")
    public Map<String, Object> incomeVsExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.incomeVsExpense(from, to);
    }

    @GetMapping("/cashbook")
    public Map<String, Object> cashbook(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reportService.cashbook(accountId,
                from == null ? LocalDate.now().minusMonths(3) : from,
                to == null ? LocalDate.now() : to, page, size);
    }

    @GetMapping("/payment-method-summary")
    public Map<String, Object> paymentMethodSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.paymentMethodSummary(from, to);
    }
}
