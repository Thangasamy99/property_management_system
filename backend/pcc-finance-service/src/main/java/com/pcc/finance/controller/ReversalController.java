package com.pcc.finance.controller;

import com.pcc.finance.dto.request.ReversalRequest;
import com.pcc.finance.dto.response.PaymentResponse;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.service.PaymentService;
import com.pcc.finance.service.RefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reversals")
@RequiredArgsConstructor
@Tag(name = "Reversals")
@SecurityRequirement(name = "bearerAuth")
public class ReversalController {

    private final RefundService refundService;
    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public PaymentResponse reverse(@Valid @RequestBody ReversalRequest req) {
        Payment p = refundService.reverse(req.paymentId(), req.reason());
        return paymentService.get(p.getId());
    }
}
