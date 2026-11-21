package com.pcc.finance.controller;

import com.pcc.finance.dto.request.DecisionRequest;
import com.pcc.finance.dto.request.ExpensePayRequest;
import com.pcc.finance.dto.request.ExpenseRequest;
import com.pcc.finance.dto.response.ExpenseResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.enums.ExpenseStatus;
import com.pcc.finance.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return expenseService.list(status, category, from, to, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public ExpenseResponse get(@PathVariable Long id) {
        return expenseService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest req) {
        return expenseService.create(req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponse approve(@PathVariable Long id) {
        return expenseService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponse reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest req) {
        return expenseService.reject(id, req == null ? null : req.note());
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('EXPENSE_PAY')")
    public ExpenseResponse pay(@PathVariable Long id, @Valid @RequestBody ExpensePayRequest req) {
        return expenseService.pay(id, req);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ExpenseResponse cancel(@PathVariable Long id) {
        return expenseService.cancel(id);
    }
}
