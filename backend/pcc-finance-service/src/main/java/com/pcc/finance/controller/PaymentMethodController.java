package com.pcc.finance.controller;

import com.pcc.finance.dto.request.PaymentMethodRequest;
import com.pcc.finance.dto.response.PaymentMethodResponse;
import com.pcc.finance.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods")
@SecurityRequirement(name = "bearerAuth")
public class PaymentMethodController {

    private final PaymentMethodService methodService;

    @GetMapping
    @PreAuthorize("hasAuthority('PAYMENT_VIEW') or hasAuthority('EXPENSE_VIEW')")
    public List<PaymentMethodResponse> list() {
        return methodService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_MANAGE')")
    public PaymentMethodResponse create(@Valid @RequestBody PaymentMethodRequest req) {
        return methodService.create(req);
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_MANAGE')")
    public PaymentMethodResponse toggle(@PathVariable Long id) {
        return methodService.toggle(id);
    }
}
