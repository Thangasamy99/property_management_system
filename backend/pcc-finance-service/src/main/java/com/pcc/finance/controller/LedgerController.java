package com.pcc.finance.controller;

import com.pcc.finance.dto.request.AdjustmentRequest;
import com.pcc.finance.dto.response.LedgerEntryResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.enums.LedgerDirection;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.service.LedgerService;
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
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Financial Ledger")
@SecurityRequirement(name = "bearerAuth")
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping
    @PreAuthorize("hasAuthority('LEDGER_VIEW')")
    public PageResponse<LedgerEntryResponse> list(
            @RequestParam(required = false) LedgerTxType type,
            @RequestParam(required = false) LedgerDirection direction,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ledgerService.list(type, direction, accountId, from, to, search, page, size);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEDGER_ADJUST')")
    public LedgerEntryResponse adjust(@Valid @RequestBody AdjustmentRequest req) {
        return ledgerService.adjust(req);
    }
}
