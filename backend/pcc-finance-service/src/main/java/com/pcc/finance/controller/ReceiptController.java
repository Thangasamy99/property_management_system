package com.pcc.finance.controller;

import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.ReceiptResponse;
import com.pcc.finance.service.ReceiptService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public PageResponse<ReceiptResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return receiptService.list(from, to, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public ReceiptResponse get(@PathVariable Long id) {
        return receiptService.get(id);
    }

    @GetMapping("/by-payment/{paymentId}")
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public ReceiptResponse byPayment(@PathVariable Long paymentId) {
        return receiptService.byPayment(paymentId);
    }
}
