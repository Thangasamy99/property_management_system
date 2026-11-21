package com.pcc.finance.controller;

import com.pcc.finance.dto.request.DecisionRequest;
import com.pcc.finance.dto.request.IncomeRequest;
import com.pcc.finance.dto.response.IncomeResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.enums.IncomeStatus;
import com.pcc.finance.service.OtherIncomeService;
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
@RequestMapping("/api/v1/other-income")
@RequiredArgsConstructor
@Tag(name = "Other Income")
@SecurityRequirement(name = "bearerAuth")
public class OtherIncomeController {

    private final OtherIncomeService incomeService;

    @GetMapping
    @PreAuthorize("hasAuthority('INCOME_VIEW')")
    public PageResponse<IncomeResponse> list(
            @RequestParam(required = false) IncomeStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return incomeService.list(status, category, from, to, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INCOME_VIEW')")
    public IncomeResponse get(@PathVariable Long id) {
        return incomeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INCOME_CREATE')")
    public IncomeResponse create(@Valid @RequestBody IncomeRequest req) {
        return incomeService.create(req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INCOME_APPROVE')")
    public IncomeResponse approve(@PathVariable Long id) {
        return incomeService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('INCOME_APPROVE')")
    public IncomeResponse reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest req) {
        return incomeService.reject(id, req == null ? null : req.note());
    }
}
