package com.pcc.finance.controller;

import com.pcc.finance.dto.request.PaymentRequest;
import com.pcc.finance.dto.request.PostPaymentRequest;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.PaymentResponse;
import com.pcc.finance.dto.response.TargetResponse;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import com.pcc.finance.enums.TargetType;
import com.pcc.finance.service.PaymentService;
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
@RequestMapping("/api/v1/land-acquisition-payments")
@RequiredArgsConstructor
@Tag(name = "Land Acquisition Payments")
@SecurityRequirement(name = "bearerAuth")
public class LandAcquisitionPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('LAND_PAYMENT_VIEW')")
    public PageResponse<PaymentResponse> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return paymentService.list(PaymentType.LAND_ACQUISITION, status, from, to, search, page, size);
    }

    @GetMapping("/targets/{acquisitionId}")
    @PreAuthorize("hasAuthority('LAND_PAYMENT_VIEW')")
    public TargetResponse target(@PathVariable Long acquisitionId) {
        return paymentService.targetStatus(TargetType.LAND_ACQUISITION, acquisitionId);
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAND_PAYMENT_CREATE')")
    public PaymentResponse create(@Valid @RequestBody PaymentRequest req) {
        return paymentService.create(req, PaymentType.LAND_ACQUISITION);
    }

    @PostMapping("/payments/{id}/post")
    @PreAuthorize("hasAuthority('LAND_PAYMENT_APPROVE')")
    public PaymentResponse post(@PathVariable Long id, @RequestBody(required = false) PostPaymentRequest req) {
        return paymentService.post(id, req == null ? new PostPaymentRequest(null, null) : req);
    }
}
